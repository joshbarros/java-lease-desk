#!/usr/bin/env python3
"""Drive lease desk through a signed application, a short unit, a liened tenant, and a cancellation."""

import base64
import json
import sys
import time
import urllib.error
import urllib.request
import uuid
from concurrent.futures import ThreadPoolExecutor

PORTS = {
    "owner": 8306,
    "building": 8307,
    "tenant": 8308,
    "floor": 8309,
    "unit": 8310,
    "application": 8311,
    "quote": 8312,
    "vet": 8313,
    "deed": 8314,
    "invoice": 8315,
    "sign": 8316,
    "vacate": 8317,
    "audit": 8318,
}
MGMT = "http://127.0.0.1:15690"
AUTH = "platform:platform"
DECIDE_DLQ = "lease.vet.decide.dlq"
MAX_INT = 2147483647
failures = []
RUN = uuid.uuid4().hex[:8]


def check(name, ok, detail=""):
    if ok:
        print(f"PASS  {name}", flush=True)
    else:
        print(f"FAIL  {name} {detail}", flush=True)
        failures.append(f"{name}: {detail}")


def call(method, url, body=None, auth=None, timeout=20):
    data = None if body is None else json.dumps(body).encode()
    request = urllib.request.Request(url, data=data, method=method)
    if data is not None:
        request.add_header("content-type", "application/json")
    if auth:
        request.add_header("Authorization", "Basic " + base64.b64encode(auth.encode()).decode())
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            raw = response.read().decode()
            return response.status, json.loads(raw) if raw else None
    except urllib.error.HTTPError as ex:
        raw = ex.read().decode()
        try:
            parsed = json.loads(raw) if raw else None
        except json.JSONDecodeError:
            parsed = raw
        return ex.code, parsed
    except (urllib.error.URLError, TimeoutError, json.JSONDecodeError, OSError):
        return 0, None


def wait_ok(url, seconds=300, auth=None):
    deadline = time.time() + seconds
    while time.time() < deadline:
        if call("GET", url, auth=auth)[0] == 200:
            return True
        time.sleep(1)
    return False


def until(label, fn, seconds=120):
    deadline = time.time() + seconds
    last = None
    while time.time() < deadline:
        last = fn()
        if last is not None:
            return last
        time.sleep(0.4)
    check(label, False, f"timed out last={last!r}")
    return None


def depth(name):
    status, body = call("GET", f"{MGMT}/api/queues/%2F/{name}", auth=AUTH)
    if status != 200 or not isinstance(body, dict):
        return None
    return body.get("messages", 0)


def grew(name, before, amount=1):
    def ready():
        now = depth(name)
        if now is not None and now >= before + amount:
            return now
        return None

    found = until(f"dlq {name}", ready, 120)
    if found is not None:
        check(f"dlq {name}", True)
    return found


def base(name):
    return f"http://127.0.0.1:{PORTS[name]}"


def ready_dir(service, path):
    def seen():
        code, body = call("GET", f"{base(service)}{path}")
        if code == 200 and isinstance(body, dict):
            return body
        return None

    return until(f"{service} {path}", seen, 120) is not None


def load_application(application_id):
    code, body = call("GET", f"{base('application')}/applications/{application_id}")
    if code == 200 and isinstance(body, dict):
        return body
    return None


def wait_application(application_id, status, reason=None, seconds=180):
    def ready():
        row = load_application(application_id)
        if not row or row.get("status") != status:
            return None
        if reason is not None and row.get("reason") != reason:
            return None
        return row

    return until(f"{application_id[:8]} {status}", ready, seconds)


def unit_row(unit_id):
    return call("GET", f"{base('unit')}/units/{unit_id}")


def hold_row(application_id):
    return call("GET", f"{base('unit')}/holds/{application_id}")


def audits(application_id):
    code, body = call("GET", f"{base('audit')}/audits/{application_id}")
    if code == 200 and isinstance(body, list):
        return body
    return None


def fact_count(application_id, kind, status):
    def ready():
        rows = audits(application_id)
        if rows is None:
            return None
        count = sum(1 for row in rows if row.get("kind") == kind and row.get("status") == status)
        if count > 0:
            return count
        return None

    return until(f"audit {kind} {status} {application_id[:8]}", ready, 90)


def open_owner(code, name):
    status, body = call("POST", f"{base('owner')}/owners", {"ownerCode": code, "name": name})
    check(f"owner {code}", status == 201 and isinstance(body, dict), body)
    if status != 201 or not isinstance(body, dict):
        return None
    owner_id = body["id"]
    if not ready_dir("application", f"/directory/owners/{owner_id}"):
        return None
    return owner_id


def open_building(code, status_name):
    status, body = call("POST", f"{base('building')}/buildings", {"buildingCode": code, "status": status_name})
    check(f"building {code}", status == 201 and isinstance(body, dict), body)
    if status != 201 or not isinstance(body, dict):
        return None
    building_id = body["id"]
    if not ready_dir("floor", f"/directory/buildings/{building_id}"):
        return None
    if not ready_dir("application", f"/directory/buildings/{building_id}"):
        return None
    return building_id


def open_tenant(code, name):
    status, body = call("POST", f"{base('tenant')}/tenants", {"tenantCode": code, "name": name})
    check(f"tenant {code}", status == 201 and isinstance(body, dict), body)
    if status != 201 or not isinstance(body, dict):
        return None
    tenant_id = body["id"]
    if not ready_dir("application", f"/directory/tenants/{tenant_id}"):
        return None
    return tenant_id


def open_floor(building_id, code, status_name):
    status, body = call("POST", f"{base('floor')}/floors", {
        "floorCode": code,
        "buildingId": building_id,
        "status": status_name,
    })
    check(f"floor {code}", status == 201 and isinstance(body, dict), body)
    if status != 201 or not isinstance(body, dict):
        return None
    floor_id = body["id"]
    if not ready_dir("application", f"/directory/floors/{floor_id}"):
        return None
    return floor_id


def open_unit(owner_id, building_id, code, slots, rate):
    def ready():
        status, body = call("POST", f"{base('unit')}/units", {
            "unitCode": code,
            "ownerId": owner_id,
            "buildingId": building_id,
            "slots": slots,
            "rentCents": rate,
        })
        if status == 201 and isinstance(body, dict):
            return body
        if status in (0, 404):
            return None
        check(f"unit {code}", False, (status, body))
        return "stop"

    found = until(f"unit {code}", ready, 120)
    if not isinstance(found, dict):
        return None
    check(f"unit {code}", found.get("openSlots") == slots and found.get("rentCents") == rate, found)
    unit_id = found["id"]
    if not ready_dir("application", f"/directory/units/{unit_id}"):
        return None
    return unit_id


def desk(prefix, slots, rate, months, tenant_code=None, building_status="OPEN", floor_status="OPEN"):
    code = tenant_code or f"{prefix}-P-{RUN}"
    owner_id = open_owner(f"{prefix}-R-{RUN}", prefix)
    building_id = open_building(f"{prefix}-S-{RUN}", building_status)
    tenant_id = open_tenant(code, prefix)
    if not owner_id or not building_id or not tenant_id:
        return None
    floor_id = open_floor(building_id, f"{prefix}-N-{RUN}", floor_status)
    unit_id = open_unit(owner_id, building_id, f"{prefix}-C-{RUN}", slots, rate)
    if not floor_id or not unit_id:
        return None
    return {
        "owner": owner_id,
        "building": building_id,
        "tenant": tenant_id,
        "floor": floor_id,
        "unit": unit_id,
        "slots": slots,
        "rate": rate,
        "months": months,
        "code": code,
    }


def start_application(world, request_id=None, owner_id=None, building_id=None, tenant_id=None, floor_id=None,
                unit_id=None, months=None):
    request_id = request_id or str(uuid.uuid4())
    status, body = call("POST", f"{base('application')}/applications", {
        "requestId": request_id,
        "ownerId": owner_id or world["owner"],
        "buildingId": building_id or world["building"],
        "tenantId": tenant_id or world["tenant"],
        "floorId": floor_id or world["floor"],
        "unitId": unit_id or world["unit"],
        "months": world["months"] if months is None else months,
    })
    return status, body, request_id


def sign(application_id, request_id=None):
    return call("POST", f"{base('application')}/applications/{application_id}/signatures", {
        "requestId": request_id or str(uuid.uuid4()),
    })


def cancel(application_id, request_id=None):
    return call("POST", f"{base('application')}/applications/{application_id}/breaks", {
        "requestId": request_id or str(uuid.uuid4()),
    })


def hold_returned(application_id, unit_id, slots, seconds=8):
    deadline = time.time() + seconds
    seen = None
    live = None
    opened = None
    while time.time() < deadline:
        row = load_application(application_id)
        live = hold_row(application_id)
        opened = unit_row(unit_id)
        seen = None if row is None else row.get("status")
        hold_status = None if live[0] != 200 or not isinstance(live[1], dict) else live[1].get("status")
        open_slots = None if opened[0] != 200 or not isinstance(opened[1], dict) else opened[1].get("openSlots")
        if seen != "RETURNED" or hold_status != "RETURNED" or open_slots != slots:
            return False, seen, live, opened
        time.sleep(0.5)
    return True, seen, live, opened


def main():
    for name in PORTS:
        check(f"health {name}", wait_ok(f"{base(name)}/actuator/health"))
    if failures:
        return 1

    check("blank owner", call("POST", f"{base('owner')}/owners", {"ownerCode": " ", "name": "North"})[0] == 400)
    check("bad building", call("POST", f"{base('building')}/buildings", {"buildingCode": f"BAD-{RUN}", "status": "open"})[0] == 400)
    check("blank tenant", call("POST", f"{base('tenant')}/tenants", {"tenantCode": " ", "name": "Ada"})[0] == 400)
    check("bad floor", call("POST", f"{base('floor')}/floors", {
        "floorCode": f"BAD-{RUN}", "buildingId": str(uuid.uuid4()), "status": "open",
    })[0] == 400)
    check("zero slots", call("POST", f"{base('unit')}/units", {
        "unitCode": f"ZERO-{RUN}", "ownerId": str(uuid.uuid4()), "buildingId": str(uuid.uuid4()),
        "slots": 0, "rentCents": 100,
    })[0] == 400)
    check("zero rate", call("POST", f"{base('unit')}/units", {
        "unitCode": f"RATE-{RUN}", "ownerId": str(uuid.uuid4()), "buildingId": str(uuid.uuid4()),
        "slots": 4, "rentCents": 0,
    })[0] == 400)
    check("blank application", call("POST", f"{base('application')}/applications", {"requestId": " "})[0] == 400)
    check("negative months", call("POST", f"{base('application')}/applications", {
        "requestId": str(uuid.uuid4()),
        "ownerId": str(uuid.uuid4()),
        "buildingId": str(uuid.uuid4()),
        "tenantId": str(uuid.uuid4()),
        "floorId": str(uuid.uuid4()),
        "unitId": str(uuid.uuid4()),
        "months": -1,
    })[0] == 400)
    check("unknown application", call("GET", f"{base('application')}/applications/{uuid.uuid4()}")[0] == 404)
    check("unknown hold", hold_row(str(uuid.uuid4()))[0] == 404)
    check("unknown quote", call("GET", f"{base('quote')}/quotes/{uuid.uuid4()}")[0] == 404)
    check("unknown signature", call("GET", f"{base('sign')}/signatures/{uuid.uuid4()}")[0] == 404)
    check("unknown vacate", call("GET", f"{base('vacate')}/vacates/{uuid.uuid4()}")[0] == 404)
    check("empty audit", call("GET", f"{base('audit')}/audits/{uuid.uuid4()}")[1] == [])
    missing = str(uuid.uuid4())
    check("missing owner", call("POST", f"{base('application')}/applications", {
        "requestId": str(uuid.uuid4()),
        "ownerId": missing,
        "buildingId": missing,
        "tenantId": missing,
        "floorId": missing,
        "unitId": missing,
        "months": 12,
    })[0] == 404)

    dup_owner = open_owner(f"DUP-R-{RUN}", "Dup")
    if dup_owner:
        check("duplicate owner", call("POST", f"{base('owner')}/owners", {
            "ownerCode": f"DUP-R-{RUN}", "name": "Again",
        })[0] == 409)
    dup_building = open_building(f"DUP-S-{RUN}", "OPEN")
    if dup_building:
        check("duplicate building", call("POST", f"{base('building')}/buildings", {
            "buildingCode": f"DUP-S-{RUN}", "status": "OPEN",
        })[0] == 409)
        check("missing building on floor", call("POST", f"{base('floor')}/floors", {
            "floorCode": f"MISS-{RUN}", "buildingId": str(uuid.uuid4()), "status": "OPEN",
        })[0] == 404)
    dup_tenant = open_tenant(f"DUP-P-{RUN}", "Dup")
    if dup_tenant:
        check("duplicate tenant", call("POST", f"{base('tenant')}/tenants", {
            "tenantCode": f"DUP-P-{RUN}", "name": "Again",
        })[0] == 409)

    happy = desk("HPY", 4, 100, 12)
    if happy:
        status, body, request_id = start_application(happy)
        check("happy create", status == 201 and isinstance(body, dict) and body.get("status") == "QUOTING", body)
        if status == 201 and isinstance(body, dict):
            application_id = body["id"]
            replay = start_application(happy, request_id=request_id)
            check("happy replay", replay[0] == 200 and isinstance(replay[1], dict) and replay[1].get("id") == application_id, replay[1])
            ready = wait_application(application_id, "READY")
            if ready:
                check("happy fee", ready.get("feeCents") == 1200 and ready.get("months") == 12 and ready.get("rentCents") == 100, ready)
                opened = unit_row(happy["unit"])
                hold = hold_row(application_id)
                quote = call("GET", f"{base('quote')}/quotes/{application_id}")
                vet = call("GET", f"{base('vet')}/vets/{application_id}")
                deed = call("GET", f"{base('deed')}/deeds/{application_id}")
                invoice = call("GET", f"{base('invoice')}/invoices/{application_id}")
                check("happy slots", opened[0] == 200 and opened[1].get("openSlots") == 3, opened[1])
                check("happy hold", hold[0] == 200 and hold[1].get("status") == "KEPT" and hold[1].get("feeCents") == 1200, hold[1])
                check("happy quote", quote[0] == 200 and quote[1].get("status") == "RATED" and quote[1].get("feeCents") == 1200, quote[1])
                check("happy vet", vet[0] == 200 and vet[1].get("status") == "ACCEPTED", vet[1])
                check("happy deed", deed[0] == 200 and deed[1].get("status") == "ISSUED" and deed[1].get("feeCents") == 1200, deed[1])
                check("happy invoice", invoice[0] == 200 and invoice[1].get("status") == "POSTED" and invoice[1].get("feeCents") == 1200, invoice[1])
                again = start_application(happy, request_id=request_id)
                check("replay stays one application", again[0] == 200 and again[1].get("id") == application_id, again[1])
                sign_id = str(uuid.uuid4())
                check("happy sign", sign(application_id, sign_id)[0] == 202)
                signed = wait_application(application_id, "SIGNED")
                if signed:
                    issued = call("GET", f"{base('sign')}/signatures/{application_id}")
                    check("sign issued", issued[0] == 200 and issued[1].get("status") == "ISSUED", issued[1])
                    check("sign replay", sign(application_id, sign_id)[0] == 200)
                    check("second sign", sign(application_id)[0] == 409)
                    cancel_id = str(uuid.uuid4())
                    check("cancel after sign", cancel(application_id, cancel_id)[0] == 202)
                    check("cancel replay", cancel(application_id, cancel_id)[0] == 200)
                    returned = wait_application(application_id, "RETURNED")
                    if returned:
                        steady, seen, live_hold, live_unit = hold_returned(application_id, happy["unit"], 4)
                        vacate = call("GET", f"{base('vacate')}/vacates/{application_id}")
                        deed_after = call("GET", f"{base('deed')}/deeds/{application_id}")
                        check("cancel stays", steady and seen == "RETURNED", {"status": seen, "hold": live_hold, "unit": live_unit})
                        check("cancel row", vacate[0] == 200 and vacate[1].get("status") == "RETURNED" and vacate[1].get("feeCents") == 1200, vacate[1])
                        check("deed remains", deed_after[0] == 200 and deed_after[1].get("status") == "ISSUED", deed_after[1])
                        check("second cancel", cancel(application_id)[0] == 409)
                        ready_facts = fact_count(application_id, "ApplicationFact", "READY")
                        if ready_facts is not None:
                            check("one ready fact", ready_facts == 1, ready_facts)
                        still = unit_row(happy["unit"])
                        check("replay did not take another slot", still[0] == 200 and still[1].get("openSlots") == 4, still[1])

    bound = desk("BND", 1, 100, 24)
    if bound:
        status, body, _request = start_application(bound)
        check("bound create", status == 201 and isinstance(body, dict), body)
        if status == 201 and isinstance(body, dict):
            ready = wait_application(body["id"], "READY")
            if ready:
                check("bound sign", sign(body["id"])[0] == 202)
                signed = wait_application(body["id"], "SIGNED")
                if signed:
                    opened = unit_row(bound["unit"])
                    hold = hold_row(body["id"])
                    check("bound fee", signed.get("feeCents") == 2400, signed)
                    check("bound slots", opened[0] == 200 and opened[1].get("openSlots") == 0, opened[1])
                    check("bound hold", hold[0] == 200 and hold[1].get("status") == "KEPT" and hold[1].get("feeCents") == 2400, hold[1])

    def cancelled_clean(prefix, months, rate, reason):
        world = desk(prefix, 4, rate, months)
        if not world:
            return
        status, body, _request = start_application(world)
        check(f"{prefix} create", status == 201 and isinstance(body, dict), body)
        if status != 201 or not isinstance(body, dict):
            return
        application_id = body["id"]
        row = wait_application(application_id, "CANCELLED", reason)
        if row:
            opened = unit_row(world["unit"])
            check(f"{prefix} quote", call("GET", f"{base('quote')}/quotes/{application_id}")[0] == 404)
            check(f"{prefix} hold", hold_row(application_id)[0] == 404)
            check(f"{prefix} slots", opened[0] == 200 and opened[1].get("openSlots") == 4, opened[1])
            check(f"{prefix} sign", sign(application_id)[0] == 409)
            check(f"{prefix} cancel", cancel(application_id)[0] == 409)

    cancelled_clean("EMP", 0, 100, "EMPTY")
    cancelled_clean("OVR", 25, 100, "OVER")
    cancelled_clean("OVF", 2, MAX_INT, "GROSS")

    short = desk("SHT", 1, 100, 12)
    if short:
        status, body, _request = start_application(short)
        check("short first", status == 201 and isinstance(body, dict), body)
        if status == 201 and isinstance(body, dict):
            first_id = body["id"]
            ready = wait_application(first_id, "READY")
            if ready:
                second_status, second, _request = start_application(short)
                check("short second", second_status == 201 and isinstance(second, dict), second)
                if second_status == 201 and isinstance(second, dict):
                    row = wait_application(second["id"], "CANCELLED", "SHORT")
                    if row:
                        quote = call("GET", f"{base('quote')}/quotes/{second['id']}")
                        opened = unit_row(short["unit"])
                        check("short quote", quote[0] == 200 and quote[1].get("status") == "RATED" and quote[1].get("feeCents") == 1200, quote[1])
                        check("short hold", hold_row(second["id"])[0] == 404)
                        check("short slots", opened[0] == 200 and opened[1].get("openSlots") == 0, opened[1])
                        check("short vet", call("GET", f"{base('vet')}/vets/{second['id']}")[0] == 404)
                        kept = hold_row(first_id)
                        check("short winner kept", kept[0] == 200 and kept[1].get("status") == "KEPT", kept[1])

    closed = desk("SHU", 4, 100, 12, building_status="CLOSED")
    if closed:
        status, body, _request = start_application(closed)
        check("closed building", status == 409, body)

    shut_floor = desk("PNL", 4, 100, 12, floor_status="CLOSED")
    if shut_floor:
        status, body, _request = start_application(shut_floor)
        check("closed floor", status == 409, body)

    other = desk("OTH", 4, 100, 12)
    if other and happy:
        status, body, _request = start_application(happy, building_id=other["building"], floor_id=other["floor"])
        check("mismatched building", status == 409, body)
        status, body, _request = start_application(happy, owner_id=other["owner"])
        check("mismatched owner", status == 409, body)
        check("missing unit", call("POST", f"{base('application')}/applications", {
            "requestId": str(uuid.uuid4()),
            "ownerId": happy["owner"],
            "buildingId": happy["building"],
            "tenantId": happy["tenant"],
            "floorId": happy["floor"],
            "unitId": str(uuid.uuid4()),
            "months": 12,
        })[0] == 404)

    before = depth(DECIDE_DLQ)
    check("lien depth", before is not None, before)
    liened = desk("ICE", 4, 100, 12, tenant_code=f"P-lien-{RUN}")
    if liened and before is not None:
        status, body, _request = start_application(liened)
        check("lien create", status == 201 and isinstance(body, dict), body)
        if status == 201 and isinstance(body, dict):
            application_id = body["id"]

            def restored():
                row = load_application(application_id)
                vet = call("GET", f"{base('vet')}/vets/{application_id}")
                vacate = call("GET", f"{base('vacate')}/vacates/{application_id}")
                hold = hold_row(application_id)
                opened = unit_row(liened["unit"])
                if not row or vet[0] != 200 or vacate[0] != 200 or hold[0] != 200 or opened[0] != 200:
                    return None
                if row.get("status") == "CANCELLED" and row.get("reason") == "LIEN" \
                        and vet[1].get("status") == "REJECTED" \
                        and vacate[1].get("status") == "FREED" \
                        and hold[1].get("status") == "RELEASED" \
                        and opened[1].get("openSlots") == 4:
                    return row
                return None

            row = until("lien vacated", restored, 180)
            if row:
                grew(DECIDE_DLQ, before)
                quote = call("GET", f"{base('quote')}/quotes/{application_id}")
                check("lien quote", quote[0] == 200 and quote[1].get("status") == "RATED" and quote[1].get("feeCents") == 1200, quote[1])

    pair = desk("TWO", 1, 100, 12)
    if pair:
        with ThreadPoolExecutor(max_workers=2) as pool:
            first = pool.submit(start_application, pair)
            second = pool.submit(start_application, pair)
            left = first.result()
            right = second.result()
        check("two creates", left[0] == 201 and right[0] == 201 and isinstance(left[1], dict) and isinstance(right[1], dict), (left[0], right[0]))
        if left[0] == 201 and right[0] == 201:
            ids = (left[1]["id"], right[1]["id"])

            def split():
                rows = [load_application(item) for item in ids]
                if any(row is None for row in rows):
                    return None
                ready_rows = [row for row in rows if row.get("status") == "READY" and row.get("feeCents") == 1200]
                short_rows = [row for row in rows if row.get("status") == "CANCELLED" and row.get("reason") == "SHORT"]
                if len(ready_rows) == 1 and len(short_rows) == 1:
                    return ready_rows[0], short_rows[0]
                return None

            found = until("one ready application", split, 180)
            if found:
                winner, loser = found
                live = hold_row(winner["id"])
                loser_hold = hold_row(loser["id"])
                opened = unit_row(pair["unit"])
                check("one hold", live[0] == 200 and live[1].get("status") == "KEPT" and live[1].get("feeCents") == 1200, live[1])
                check("short skipped hold", loser_hold[0] == 404, loser_hold)
                check("one slot take", opened[0] == 200 and opened[1].get("openSlots") == 0, opened[1])
                winner_ready = fact_count(winner["id"], "ApplicationFact", "READY")
                loser_short = fact_count(loser["id"], "ApplicationFact", "CANCELLED")
                if winner_ready is not None:
                    check("winner ready once", winner_ready == 1, winner_ready)
                if loser_short is not None:
                    check("loser short once", loser_short == 1, loser_short)

    race = desk("RCE", 4, 100, 12)
    if race:
        status, body, _request = start_application(race)
        check("race create", status == 201 and isinstance(body, dict), body)
        if status == 201 and isinstance(body, dict):
            application_id = body["id"]
            ready = wait_application(application_id, "READY")
            if ready:
                with ThreadPoolExecutor(max_workers=2) as pool:
                    signed = pool.submit(sign, application_id)
                    cancelled = pool.submit(cancel, application_id)
                    sign_result = signed.result()
                    cancel_result = cancelled.result()
                check("race cancel", cancel_result[0] == 202, cancel_result)
                check("race sign", sign_result[0] in (202, 409), sign_result)
                done = wait_application(application_id, "RETURNED")
                if done:
                    steady, seen, live_hold, live_unit = hold_returned(application_id, race["unit"], 4)
                    check("race stays returned", steady and seen == "RETURNED", {"status": seen, "hold": live_hold, "unit": live_unit})

    check("unknown unit", unit_row(str(uuid.uuid4()))[0] == 404)
    check("unknown deed", call("GET", f"{base('deed')}/deeds/{uuid.uuid4()}")[0] == 404)

    if failures:
        print(f"{len(failures)} failed", flush=True)
        for failure in failures:
            print(f" - {failure}", flush=True)
        return 1
    print("lease desk smoke passed", flush=True)
    return 0


if __name__ == "__main__":
    sys.exit(main())
