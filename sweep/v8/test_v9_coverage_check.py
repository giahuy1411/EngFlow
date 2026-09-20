import hashlib
import json
import tempfile
import unittest
from pathlib import Path

from v9_coverage_check import assess, matches


class CurrentRunCoverageTest(unittest.TestCase):
    def setUp(self):
        self.workspace = tempfile.TemporaryDirectory()
        self.addCleanup(self.workspace.cleanup)
        self.root = Path(self.workspace.name)
        self.inventory = {"endpoints": [{"httpMethod": "GET", "path": "/api/items/{id:[0-9]+}"}]}
        self.probe = {
            "name": "valid item",
            "method": "GET",
            "path": "/api/items/1",
            "code": 200,
            "expectedStatuses": [200],
            "pass": True,
            "assertions": [{"name": "response contract", "pass": True}],
        }

    def write(self, name, value):
        path = self.root / name
        path.write_text(json.dumps(value), encoding="utf-8")
        return {"path": name, "sha256": hashlib.sha256(path.read_bytes()).hexdigest()}

    def manifest(self, run_id="run-1"):
        envelope = {"runId": run_id, "artifactId": "image-1", "probes": [self.probe]}
        self.write("manifest.json", {
            "runId": "run-1", "artifactId": "image-1",
            "inventory": self.write("inventory.json", self.inventory),
            "probeFiles": [self.write("probes.json", envelope)],
        })
        return self.root / "manifest.json"

    def test_current_success_covers_mapping(self):
        self.assertTrue(assess(self.manifest())["pass"])

    def test_failed_assertion_never_counts_as_coverage(self):
        self.probe["assertions"][0]["pass"] = False
        result = assess(self.manifest())
        self.assertFalse(result["pass"])
        self.assertEqual(0, result["coveredCount"])

    def test_expected_404_is_negative_control_not_functional_coverage(self):
        self.probe["code"] = 404
        self.probe["expectedStatuses"] = [404]
        result = assess(self.manifest())
        self.assertFalse(result["pass"])
        self.assertEqual(1, result["negativeControlCount"])

    def test_stale_run_is_rejected(self):
        with self.assertRaises(ValueError):
            assess(self.manifest("old-run"))

    def test_changed_evidence_is_rejected(self):
        manifest = self.manifest()
        (self.root / "probes.json").write_text("{}", encoding="utf-8")
        with self.assertRaises(ValueError):
            assess(manifest)

    def test_empty_inventory_is_rejected(self):
        self.inventory["endpoints"] = []
        with self.assertRaises(ValueError):
            assess(self.manifest())

    def test_regex_parameter_is_respected(self):
        self.assertFalse(matches("/api/items/{id:[0-9]+}", "/api/items/word"))
        self.assertTrue(matches("/api/items/{id:[0-9]+}", "/api/items/123"))

    def test_static_route_takes_precedence(self):
        self.inventory = {"endpoints": [
            {"httpMethod": "GET", "path": "/api/items/search"},
            {"httpMethod": "GET", "path": "/api/items/{id}"},
        ]}
        self.probe["path"] = "/api/items/search"
        result = assess(self.manifest())
        self.assertEqual(1, result["coveredCount"])
        self.assertEqual([{"method": "GET", "path": "/api/items/{id}"}], result["uncovered"])

    def test_legacy_probe_file_is_not_accepted(self):
        manifest = self.manifest()
        record = json.loads(manifest.read_text(encoding="utf-8"))
        record["probeFiles"] = [self.write("probes.json", [self.probe])]
        self.write("manifest.json", record)
        with self.assertRaises(TypeError):
            assess(manifest)


if __name__ == "__main__":
    unittest.main()
