#!/usr/bin/env python3
"""Usage: python3 tools/generate-openvox-data.py /path/to/openvox"""
import os
import re
import sys

OUT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                   "src", "main", "resources", "openvox")


def clean(text, limit=600):
    text = re.sub(r"\s+", " ", text or "").strip()
    text = text.replace("\\", "\\\\").replace("\t", " ")
    if len(text) > limit:
        cut = text[:limit].rsplit(" ", 1)[0]
        text = cut + "…"
    return text


def leading_comment(lines, index):
    out = []
    i = index - 1
    while i >= 0:
        line = lines[i].rstrip("\n")
        if line.startswith("#"):
            out.append(line.lstrip("#").strip())
            i -= 1
        elif line.strip() == "":
            if out:
                break
            i -= 1
        else:
            break
    out.reverse()
    summary = []
    for line in out:
        if not line and summary:
            break
        if line.startswith("@") or line.startswith("```"):
            break
        summary.append(line)
    return " ".join(summary)


PARAM = re.compile(r"^\s*(required_param|optional_param|repeated_param|optional_repeated_param|param)\s+"
                   r"(['\"])(?P<type>.+?)\2\s*,\s*:(?P<name>\w+)")
BLOCK_PARAM = re.compile(r"^\s*(required_block_param|optional_block_param|block_param)")
RETURN_TYPE = re.compile(r"^\s*return_type\s+(['\"])(?P<type>.+?)\1")
DISPATCH = re.compile(r"^\s*dispatch\s+:(?P<name>\w+)\s+do")


def modern_functions(root):
    directory = os.path.join(root, "lib", "puppet", "functions")
    for name in sorted(os.listdir(directory)):
        if not name.endswith(".rb"):
            continue
        path = os.path.join(directory, name)
        with open(path, encoding="utf-8", errors="replace") as handle:
            lines = handle.readlines()

        create = next((i for i, l in enumerate(lines)
                       if "Puppet::Functions.create_function" in l), None)
        if create is None:
            continue
        match = re.search(r"create_function\(\s*:'?([\w:]+)'?", lines[create])
        if not match:
            continue
        function = match.group(1)
        doc = leading_comment(lines, create)

        signatures, current, returns = [], None, ""
        for line in lines[create:]:
            dispatch = DISPATCH.match(line)
            if dispatch:
                if current is not None:
                    signatures.append(", ".join(current))
                current = []
                continue
            if current is None:
                continue
            param = PARAM.match(line)
            if param:
                optional = param.group(1).startswith("optional")
                repeated = "repeated" in param.group(1)
                rendered = "%s $%s" % (param.group("type"), param.group("name"))
                if repeated:
                    rendered = "*" + rendered
                current.append("[%s]" % rendered if optional else rendered)
                continue
            if BLOCK_PARAM.match(line):
                current.append("&block")
                continue
            found = RETURN_TYPE.match(line)
            if found:
                returns = found.group("type")
        if current is not None:
            signatures.append(", ".join(current))

        yield function, [s for s in signatures if s is not None], returns, doc


LEGACY = re.compile(r"newfunction\(\s*:(?P<name>\w+)\s*(?P<rest>.*)")


def legacy_functions(root):
    directory = os.path.join(root, "lib", "puppet", "parser", "functions")
    if not os.path.isdir(directory):
        return
    for name in sorted(os.listdir(directory)):
        if not name.endswith(".rb"):
            continue
        with open(os.path.join(directory, name), encoding="utf-8", errors="replace") as handle:
            text = handle.read()
        match = LEGACY.search(text)
        if not match:
            continue
        doc = ""
        doc_match = re.search(r":doc\s*=>\s*<<-?['\"]?(\w+)['\"]?\n(.*?)\n\s*\1", text, re.S)
        if doc_match:
            doc = doc_match.group(2)
        else:
            inline = re.search(r":doc\s*=>\s*(['\"])(.*?)\1", text, re.S)
            if inline:
                doc = inline.group(2)
        yield match.group("name"), [""], "", doc


NEWTYPE = re.compile(r"(?:Puppet::)?Type\.newtype\(\s*:(?P<name>\w+)")
ATTRIBUTE = re.compile(r"^\s*(?:\w+\s*=\s*)?(?:[\w:.()]*\.)?"
                       r"(?P<kind>newparam|newproperty|newmetaparam|newcheck)\s*\(?\s*:(?P<name>\w+)")
NEWVALUES = re.compile(r"^\s*newvalues\((?P<values>.+)\)")
DESC_INLINE = re.compile(r"^\s*desc\s+(['\"])(?P<text>.*?)\1\s*$")
DESC_OPEN = re.compile(r"^\s*desc\s+(?P<quote>['\"])(?P<text>.*)$")
DESC_HEREDOC = re.compile(r"^\s*desc\s+<<[-~]?['\"]?(?P<tag>\w+)['\"]?")


def read_desc(lines, start):
    line = lines[start]
    inline = DESC_INLINE.match(line)
    if inline:
        return inline.group("text")

    heredoc = DESC_HEREDOC.match(line)
    if heredoc:
        tag, collected, cursor = heredoc.group("tag"), [], start + 1
        while cursor < len(lines) and lines[cursor].strip() != tag:
            collected.append(lines[cursor])
            cursor += 1
        return " ".join(collected)

    opened = DESC_OPEN.match(line)
    if opened:
        quote = opened.group("quote")
        collected, cursor = [opened.group("text")], start + 1
        while cursor < len(lines) and quote not in lines[cursor]:
            collected.append(lines[cursor])
            cursor += 1
        if cursor < len(lines):
            collected.append(lines[cursor].split(quote)[0])
        return " ".join(collected)

    return ""


NEWVALUE = re.compile(r"^\s*newvalue\(\s*:(?P<value>\w+)")


def attributes_in(lines):
    attributes = []
    index = 0
    while index < len(lines):
        attribute = ATTRIBUTE.match(lines[index])
        if attribute:
            attr_name = attribute.group("name")
            kind = {"newparam": "parameter",
                    "newproperty": "property",
                    "newmetaparam": "metaparameter", "newcheck": "parameter"}[attribute.group("kind")]
            desc, values = "", []
            depth = index + 1
            while depth < len(lines) and depth < index + 120:
                line = lines[depth]
                if ATTRIBUTE.match(line):
                    break
                if not desc:
                    desc = read_desc(lines, depth)
                allowed = NEWVALUES.match(line)
                if allowed:
                    values += re.findall(r":(\w+)", allowed.group("values"))
                single = NEWVALUE.match(line)
                if single:
                    values.append(single.group("value"))
                depth += 1
            attributes.append((attr_name, kind, list(dict.fromkeys(values)), desc))
            index += 1
            continue

        if re.match(r"^\s*(?:[\w:.()]*\.)?ensurable\s+do", lines[index]):
            desc, values, depth = "", [], index + 1
            while depth < len(lines) and depth < index + 120:
                if ATTRIBUTE.match(lines[depth]):
                    break
                if not desc:
                    desc = read_desc(lines, depth)
                single = NEWVALUE.match(lines[depth])
                if single:
                    values.append(single.group("value"))
                depth += 1
            attributes.append(("ensure", "property", list(dict.fromkeys(values)), desc))
        index += 1
    return attributes


def resource_types(root):
    directory = os.path.join(root, "lib", "puppet", "type")
    for name in sorted(os.listdir(directory)):
        if not name.endswith(".rb"):
            continue
        with open(os.path.join(directory, name), encoding="utf-8", errors="replace") as handle:
            lines = handle.readlines()
        text = "".join(lines)
        found = NEWTYPE.search(text)
        if not found:
            continue
        type_name = found.group("name")

        type_doc = ""
        doc_match = re.search(r"@doc\s*=\s*(['\"])(.*?)\1", text, re.S)
        if doc_match:
            type_doc = doc_match.group(2)

        attributes = attributes_in(lines)

        extra = os.path.join(directory, name[:-3])
        if os.path.isdir(extra):
            for part in sorted(os.listdir(extra)):
                if not part.endswith(".rb"):
                    continue
                with open(os.path.join(extra, part), encoding="utf-8", errors="replace") as handle:
                    attributes += attributes_in(handle.readlines())

        providers = os.path.join(root, "lib", "puppet", "provider", type_name)
        if os.path.isdir(providers) and not any(a[0] == "provider" for a in attributes):
            attributes.append(("provider", "parameter", [], "The backend used to manage this resource."))
        yield type_name, type_doc, attributes


def metaparameters(root):
    path = os.path.join(root, "lib", "puppet", "type.rb")
    with open(path, encoding="utf-8", errors="replace") as handle:
        lines = handle.readlines()
    for index, line in enumerate(lines):
        found = re.match(r"\s*newmetaparam\(\s*:(?P<name>\w+)", line)
        if not found:
            continue
        desc = ""
        for offset in range(index + 1, min(index + 60, len(lines))):
            desc = read_desc(lines, offset)
            if desc:
                break
        yield found.group("name"), desc


def data_types(root):
    directory = os.path.join(root, "lib", "puppet", "pops", "types")
    names = set()
    for name in os.listdir(directory):
        if not name.endswith(".rb"):
            continue
        with open(os.path.join(directory, name), encoding="utf-8", errors="replace") as handle:
            names.update(re.findall(r"^\s*class P(\w+?)Type\b", handle.read(), re.M))
    skip = {"Abstract", "AbstractTimeData", "Meta", "Anonymous", "TypeWithContained",
            "Nested", "TypeReference", "TypeAlias", "TypeSet", "Object", "ObjectTypeExtension"}
    extras = {"Deferred", "Error", "RichData", "Target"}
    return sorted((names | extras) - skip)


DATA_TYPE_DOCS = {
    "Any": "The top of the type system. Every value is an Any.",
    "Array": "An ordered collection. `Array[String]`, `Array[Integer, 1, 10]` for a size range.",
    "Binary": "A sequence of bytes, normally produced by `binary_file()` or base64 content.",
    "Boolean": "`true` or `false`.",
    "Callable": "A lambda or function reference. `Callable[String, Integer]` describes its parameters.",
    "CatalogEntry": "Anything that can appear in a catalog: a Resource or a Class.",
    "Class": "A reference to a Puppet class. `Class['apache']`.",
    "Collection": "An Array or a Hash.",
    "Default": "The type of the `default` keyword, used in case options and selectors.",
    "Deferred": "A function call evaluated on the agent rather than in the catalog.",
    "Enum": "One of a fixed set of strings. `Enum['running', 'stopped']`.",
    "Error": "An error value, used with `Deferred` and error handling.",
    "Float": "A floating point number. `Float[0.0, 1.0]` constrains the range.",
    "Hash": "A key-value collection. `Hash[String, Integer]`, with an optional size range.",
    "Init": "Accepts whatever the given type can be created from. `Init[Integer, 8]` parses octal strings.",
    "Integer": "A whole number. `Integer[1, 65535]` constrains the range.",
    "Iterable": "Anything that can be iterated: a collection, a String, an Integer range.",
    "Iterator": "A lazy sequence produced by `reverse_each`, `step` and friends.",
    "NotUndef": "Any value except `undef`. `NotUndef[String]` narrows it further.",
    "Numeric": "An Integer or a Float.",
    "Optional": "The given type or `undef`. `Optional[String[1]]`.",
    "Pattern": "A String matching at least one of the given regular expressions.",
    "Regexp": "A regular expression value. `Regexp[/^a/]` matches one specific pattern.",
    "Resource": "A reference to any resource. `Resource['file', '/etc/motd']`.",
    "RichData": "Any value that survives catalog serialisation, including Sensitive and Deferred.",
    "Scalar": "A single value: String, Numeric, Boolean or Regexp.",
    "ScalarData": "A Scalar that is safe to serialise as data: String, Numeric or Boolean.",
    "SemVer": "A semantic version. `SemVer['1.0.0', '2.0.0']` constrains the range.",
    "SemVerRange": "A semantic version range, as used in module dependencies.",
    "Sensitive": "Wraps a value so it is redacted from logs and reports. `Sensitive[String[1]]`.",
    "String": "Text. `String[1]` requires at least one character; `String[1, 20]` bounds it.",
    "Struct": "A Hash with a known shape. `Struct[{ host => String, Optional[port] => Integer }]`.",
    "Target": "A Bolt target. Only meaningful in plans.",
    "Timespan": "A duration, produced by `Timespan(...)` or a `{ hours => 2 }` hash.",
    "Timestamp": "A point in time, produced by `Timestamp(...)`.",
    "Tuple": "An Array with a known shape. `Tuple[String, Integer, 2, 4]`.",
    "Type": "The type of a type. `Type[String]`.",
    "URI": "A uniform resource identifier, with optional constraints on its parts.",
    "Undef": "The type of `undef`.",
    "Unit": "Accepts any value and converts nothing; rarely used directly.",
    "Variant": "Any one of the given types. `Variant[String, Integer]`.",
}


def write(name, rows):
    os.makedirs(OUT, exist_ok=True)
    path = os.path.join(OUT, name)
    with open(path, "w", encoding="utf-8") as handle:
        handle.write("# Generated by tools/generate-openvox-data.py from the OpenVox sources.\n")
        for row in rows:
            handle.write("\t".join(row) + "\n")
    print("%-22s %4d rows" % (name, len(rows)))


def compatible_function(name, signatures, doc):
    if name == "regsubst":
        encoding = ", [Enum['N','E','S','U'] $encoding]"
        signatures = list(dict.fromkeys([s.removesuffix(encoding) for s in signatures] + signatures))
        doc += " OpenVox 8 accepts an ignored fifth encoding argument; OpenVox 9 removes it. Omit it for compatibility with both versions."
    return signatures, doc


def main():
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    root = sys.argv[1]

    functions = []
    for name, signatures, returns, doc in modern_functions(root):
        signatures, doc = compatible_function(name, signatures, clean(doc))
        functions.append((name, ";".join(dict.fromkeys(signatures)), returns, doc))
    modern_names = {f[0] for f in functions}
    for name, signatures, returns, doc in legacy_functions(root):
        if name not in modern_names:
            functions.append((name, "", "", clean(doc)))
    write("functions.tsv", sorted(functions))

    types, attributes = [], []
    for name, doc, attrs in resource_types(root):
        types.append((name, clean(doc)))
        for attr_name, kind, values, desc in attrs:
            desc = clean(desc, 300)
            if (name, attr_name) == ("file", "content"):
                desc += " OpenVox 9 writes checksum-looking strings literally; OpenVox 8 can retrieve them from a filebucket."
            attributes.append((name, attr_name, kind, ",".join(values), desc))
    write("types.tsv", sorted(types))
    write("type-attributes.tsv", sorted(attributes))

    write("metaparameters.tsv", sorted((n, clean(d, 300)) for n, d in metaparameters(root)))
    write("datatypes.tsv", sorted((n, DATA_TYPE_DOCS.get(n, "")) for n in data_types(root)))


if __name__ == "__main__":
    main()
