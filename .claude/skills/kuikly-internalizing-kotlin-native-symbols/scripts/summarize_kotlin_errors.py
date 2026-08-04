#!/usr/bin/env python3
"""汇总 Kotlin 编译日志中的 e: 错误，便于内部化工作按根因收敛。"""

from __future__ import annotations

import argparse
import json
import re
import sys
from collections import Counter
from pathlib import Path

FILE_ERROR_RE = re.compile(r"^e: file://(?P<file>.+?):(?P<line>\d+):(?P<col>\d+) (?P<message>.*)$")
GENERIC_ERROR_RE = re.compile(r"^e:\s*(?P<message>.*)$")

CATEGORY_PATTERNS = [
    ("误改局部变量", re.compile(r"not applicable to 'local variable'", re.IGNORECASE)),
    ("误改嵌套或受限上下文", re.compile(r"not applicable inside", re.IGNORECASE)),
    ("public 暴露 internal 类型", re.compile(r"exposes its internal", re.IGNORECASE)),
    ("public inline 访问 non-public API", re.compile(r"public-API inline function cannot access non-public-API|non-public-API", re.IGNORECASE)),
    ("expect/actual 不一致", re.compile(r"'actual'.*no corresponding expected declaration|expected declaration", re.IGNORECASE)),
]


def classify(message: str) -> str:
    for category, pattern in CATEGORY_PATTERNS:
        if pattern.search(message):
            return category
    return "其他"


def parse_errors(text: str) -> list[dict[str, object]]:
    errors: list[dict[str, object]] = []
    for raw_line in text.splitlines():
        line = raw_line.rstrip()
        if not line.startswith("e:"):
            continue

        file_match = FILE_ERROR_RE.match(line)
        if file_match:
            errors.append(
                {
                    "file": file_match.group("file"),
                    "line": int(file_match.group("line")),
                    "col": int(file_match.group("col")),
                    "message": file_match.group("message"),
                }
            )
            continue

        generic_match = GENERIC_ERROR_RE.match(line)
        if generic_match:
            errors.append(
                {
                    "file": None,
                    "line": None,
                    "col": None,
                    "message": generic_match.group("message"),
                }
            )
    return errors


def build_summary(errors: list[dict[str, object]]) -> dict[str, object]:
    by_category = Counter()
    by_file = Counter()
    by_message = Counter()

    for error in errors:
        message = str(error["message"])
        category = classify(message)
        by_category[category] += 1
        by_message[message] += 1
        if error["file"]:
            by_file[str(error["file"])] += 1

    return {
        "total_errors": len(errors),
        "by_category": dict(by_category.most_common()),
        "by_file": dict(by_file.most_common()),
        "by_message": dict(by_message.most_common()),
        "errors": [
            {
                **error,
                "category": classify(str(error["message"])),
            }
            for error in errors
        ],
    }


def print_human(summary: dict[str, object], top: int) -> None:
    print(f"总错误数: {summary['total_errors']}")
    print()

    print("按类别统计:")
    for category, count in list(summary["by_category"].items())[:top]:
        print(f"  - {category}: {count}")
    print()

    print("按文件统计:")
    for file_path, count in list(summary["by_file"].items())[:top]:
        print(f"  - {count:>3} {file_path}")
    print()

    print("按错误消息统计:")
    for message, count in list(summary["by_message"].items())[:top]:
        print(f"  - {count:>3} {message}")
    print()

    print("前几条错误示例:")
    for error in summary["errors"][:top]:
        location = "<unknown>"
        if error["file"]:
            location = f"{error['file']}:{error['line']}:{error['col']}"
        print(f"  - [{error['category']}] {location} -> {error['message']}")


def main() -> int:
    parser = argparse.ArgumentParser(description="汇总 Kotlin 编译日志中的错误")
    parser.add_argument("logfile", help="编译日志路径，或使用 - 表示从 stdin 读取")
    parser.add_argument("--json", action="store_true", dest="as_json", help="以 JSON 输出")
    parser.add_argument("--top", type=int, default=10, help="每个分组最多展示多少条")
    args = parser.parse_args()

    if args.logfile == "-":
        text = sys.stdin.read()
    else:
        text = Path(args.logfile).read_text(encoding="utf-8", errors="ignore")

    errors = parse_errors(text)
    summary = build_summary(errors)

    if args.as_json:
        print(json.dumps(summary, ensure_ascii=False, indent=2))
    else:
        print_human(summary, max(args.top, 1))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
