import importlib.util
import pathlib
import tempfile
import unittest

spec = importlib.util.spec_from_file_location("generator", pathlib.Path(__file__).with_name("generate-openvox-data.py"))
generator = importlib.util.module_from_spec(spec)
spec.loader.exec_module(generator)


class FunctionCompatibilityTest(unittest.TestCase):
    def test_shared_signature_precedes_openvox8_encoding_overload(self):
        shared = "String $target, String $pattern, String $replacement, [String $flags]"
        legacy = shared + ", [Enum['N','E','S','U'] $encoding]"
        signatures, doc = generator.compatible_function("regsubst", [legacy], "Replace text.")
        self.assertEqual(signatures, [shared, legacy])
        self.assertIn("OpenVox 8", doc)
        self.assertIn("OpenVox 9 removes it", doc)

    def test_openvox9_signature_is_not_duplicated(self):
        signatures = ["String $target, String $pattern, String $replacement"]
        actual, _ = generator.compatible_function("regsubst", signatures, "Replace text.")
        self.assertEqual(actual, signatures)


class AttributeExtractionTest(unittest.TestCase):
    def test_exec_checks_and_parenthesis_free_parameters(self):
        lines = '''newcheck(:creates) do
  desc "Skip when the file exists."
end
newparam :hasrestart do
  desc "Supports restart."
  newvalues(:true, :false)
end
'''.splitlines()
        self.assertEqual(generator.attributes_in(lines), [
            ("creates", "parameter", [], "Skip when the file exists."),
            ("hasrestart", "parameter", ["true", "false"], "Supports restart."),
        ])

    def test_provider_parameter_is_included_only_for_provider_types(self):
        with tempfile.TemporaryDirectory() as directory:
            root = pathlib.Path(directory)
            types = root / "lib/puppet/type"
            types.mkdir(parents=True)
            (types / "package.rb").write_text("Puppet::Type.newtype(:package) do\nend\n")
            (types / "notify.rb").write_text("Puppet::Type.newtype(:notify) do\nend\n")
            (root / "lib/puppet/provider/package").mkdir(parents=True)
            attributes = {name: attrs for name, _, attrs in generator.resource_types(root)}
            self.assertIn("provider", [attr[0] for attr in attributes["package"]])
            self.assertNotIn("provider", [attr[0] for attr in attributes["notify"]])
