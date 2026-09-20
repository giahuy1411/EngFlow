import argparse
import hashlib
import json
import re
from pathlib import Path


def read_json(path):
    return json.loads(path.read_text(encoding="utf-8-sig"))


def checked_file(root, entry):
    path = (root / entry["path"]).resolve()
    if not path.is_relative_to(root):
        raise ValueError("Evidence path escapes manifest directory")
    if hashlib.sha256(path.read_bytes()).hexdigest() != entry["sha256"]:
        raise ValueError("Evidence hash mismatch: " + entry["path"])
    return path


def segments(path):
    return path.split("?", 1)[0].strip("/").split("/")


def matches(template, path):
    expected = segments(template)
    actual = segments(path)
    for index, segment in enumerate(expected):
        if segment == "**" or segment.startswith("{*"):
            return index == len(expected) - 1
        if index >= len(actual):
            return False
        if segment.startswith("{") and segment.endswith("}"):
            rule = segment[1:-1].partition(":")[2]
            if rule:
                if re.fullmatch(rule, actual[index]) is None:
                    return False
            elif not actual[index]:
                return False
        elif segment != actual[index]:
            return False
    return len(expected) == len(actual)


def assess(manifest_path):
    manifest_path = Path(manifest_path).resolve()
    manifest = read_json(manifest_path)
    root = manifest_path.parent
    run_id = manifest["runId"]
    artifact_id = manifest["artifactId"]
    if not isinstance(run_id, str) or not run_id.strip():
        raise ValueError("Nonempty runId required")
    if not isinstance(artifact_id, str) or not artifact_id.strip():
        raise ValueError("Nonempty artifactId required")
    inventory = read_json(checked_file(root, manifest["inventory"]))
    endpoints = inventory["endpoints"]
    if not endpoints:
        raise ValueError("Empty inventory cannot pass coverage")
    mapping_keys = {(entry["httpMethod"], entry["path"]) for entry in endpoints}
    covered = set()
    failures = []
    positive_count = 0
    negative_count = 0
    for entry in manifest["probeFiles"]:
        envelope = read_json(checked_file(root, entry))
        if envelope["runId"] != run_id or envelope["artifactId"] != artifact_id:
            raise ValueError("Stale or mismatched probe artifact: " + entry["path"])
        for probe in envelope["probes"]:
            status = probe.get("code")
            expected = probe.get("expectedStatuses", [])
            assertions = probe.get("assertions", [])
            valid = (
                probe.get("pass") is True
                and type(status) is int
                and 100 <= status <= 599
                and status in expected
                and isinstance(assertions, list)
                and bool(assertions)
                and all(isinstance(item, dict) and item.get("pass") is True
                        and bool(item.get("name")) for item in assertions)
            )
            if not valid:
                failures.append(probe.get("name", "unnamed"))
                continue
            hits = [key for key in mapping_keys
                    if key[0] == probe["method"]
                    and matches(key[1], probe["path"])]
            exact = [key for key in hits if key[1] == probe["path"].split("?", 1)[0]]
            if exact:
                hits = exact
            if len(hits) != 1:
                failures.append(probe.get("name", "unnamed") + ": ambiguous/unmapped route")
                continue
            if 200 <= status < 300:
                covered.add(hits[0])
                positive_count += 1
            else:
                negative_count += 1
    missing = sorted(mapping_keys - covered)
    return {
        "runId": run_id,
        "artifactId": artifact_id,
        "inventoryCount": len(mapping_keys),
        "coveredCount": len(covered),
        "positiveProbeCount": positive_count,
        "negativeControlCount": negative_count,
        "uncovered": [{"method": method, "path": path} for method, path in missing],
        "failedProbes": failures,
        "pass": not missing and not failures,
    }


def main():
    parser = argparse.ArgumentParser(description="Current-run successful mapping coverage, not a full security audit")
    parser.add_argument("--manifest", required=True)
    args = parser.parse_args()
    try:
        result = assess(args.manifest)
    except (OSError, ValueError, KeyError, TypeError, re.error) as error:
        print(json.dumps({"pass": False, "error": str(error)}))
        return 2
    print(json.dumps(result, indent=2))
    return 0 if result["pass"] else 1


if __name__ == "__main__":
    raise SystemExit(main())
