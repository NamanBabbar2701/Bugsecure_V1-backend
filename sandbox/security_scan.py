import json
import os
import re
import sys


def iter_files(root: str):
    for dirpath, _, filenames in os.walk(root):
        for fn in filenames:
            yield os.path.join(dirpath, fn)


def safe_read_text(path: str, limit_chars: int = 200_000) -> str:
    try:
        with open(path, "rb") as f:
            data = f.read(limit_chars + 1)
        # Best-effort decode
        return data.decode("utf-8", errors="replace")
    except Exception as e:
        return f"<<read_error: {e}>>"


PATTERNS = [
    # XSS / script injection
    ("xss", re.compile(r"<\\s*script\\b", re.IGNORECASE)),
    ("xss", re.compile(r"on\\w+\\s*=", re.IGNORECASE)),
    # SQL injection-ish
    ("sql_injection", re.compile(r"\\b(select|union\\s+select)\\b", re.IGNORECASE)),
    ("sql_injection", re.compile(r"\\b(or)\\s+1\\s*=\\s*1\\b", re.IGNORECASE)),
    # Command execution
    ("command_execution", re.compile(r"\\b(exec|system|popen|spawn|subprocess\\b)", re.IGNORECASE)),
    # Python eval/exec
    ("command_execution", re.compile(r"\\b(eval|exec)\\b", re.IGNORECASE)),
    # Path traversal
    ("path_traversal", re.compile(r"\\.\\./")),
    ("path_traversal", re.compile(r"%2e%2e", re.IGNORECASE)),
    # SSRF hints (offline scan)
    ("ssrf", re.compile(r"(http://|https://)\\w+", re.IGNORECASE)),
    ("ssrf", re.compile(r"\\blocalhost\\b", re.IGNORECASE)),
]


def scan_text(text: str):
    findings = []
    for category, pat in PATTERNS:
        for m in pat.finditer(text):
            snippet = text[max(0, m.start() - 40): min(len(text), m.end() + 40)].replace("\\n", "\\\\n")
            findings.append(
                {
                    "category": category,
                    "match": m.group(0),
                    "snippet": snippet,
                    "offset": m.start(),
                }
            )
    return findings


def main():
    if len(sys.argv) < 2:
        print(json.dumps({"ok": False, "error": "Usage: security_scan.py <rootDir>"}))
        return

    root = sys.argv[1]
    if not os.path.isdir(root):
        print(json.dumps({"ok": False, "error": f"Root directory not found: {root}"}))
        return

    # Keep scan bounded so the sandbox can’t be DoS’d by huge submissions.
    max_files = 5000
    max_total_chars = 10_000_000

    total_chars = 0
    file_count = 0
    all_findings = []

    allowed_exts = {".js", ".ts", ".tsx", ".jsx", ".py", ".java", ".txt", ".html", ".css", ".json", ".md", ".sh", ".rb", ".php", ".go", ".c", ".cpp"}

    for path in iter_files(root):
        file_count += 1
        if file_count > max_files:
            break
        _, ext = os.path.splitext(path)
        if ext.lower() not in allowed_exts:
            continue

        text = safe_read_text(path)
        total_chars += len(text)
        if total_chars > max_total_chars:
            break

        findings = scan_text(text)
        if findings:
            all_findings.append({"file": path, "findings": findings[:50]})

    out = {"ok": True, "filesScanned": file_count, "findings": all_findings}
    print(json.dumps(out))


if __name__ == "__main__":
    main()

