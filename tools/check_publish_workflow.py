"""Check the workflow's actual shell steps without credentials or uploads.

Run with Python 3; optionally pass the path to Bash (e.g. Git Bash on Windows).
"""

import os
from pathlib import Path
import subprocess
import sys
import tempfile
import textwrap


workflow = (Path(__file__).resolve().parents[1] / ".github/workflows/publish.yml").read_text()
bash = sys.argv[1] if len(sys.argv) > 1 else "bash"


def step_script(name):
    step = workflow.split(f"      - name: {name}\n", 1)[1].split("\n      - ", 1)[0]
    return textwrap.dedent(step.split("        run: |\n", 1)[1])


def run_step(name, directory, env):
    return subprocess.run(
        [bash, "--noprofile", "--norc", "-eo", "pipefail", "-c", step_script(name)],
        cwd=directory, env={**os.environ, **env}, capture_output=True, text=True,
    )


cases = [
    ("main snapshot", "push", "branch", "main", "", "0.1.0-SNAPSHOT", "0.1.0-SNAPSHOT"),
    ("release tag", "push", "tag", "v0.1.0", "", "0.1.0-SNAPSHOT", "0.1.0"),
    ("prerelease tag", "push", "tag", "v0.2.0-rc.1", "", "0.1.0-SNAPSHOT", "0.2.0-rc.1"),
    ("manual default", "workflow_dispatch", "branch", "main", "", "0.1.0-SNAPSHOT", "0.1.0-SNAPSHOT"),
    ("manual snapshot", "workflow_dispatch", "branch", "main", "0.2.0-SNAPSHOT", "0.1.0-SNAPSHOT", "0.2.0-SNAPSHOT"),
    ("manual release", "workflow_dispatch", "branch", "main", "0.2.0", "0.1.0-SNAPSHOT", "0.2.0"),
    ("tag precedence", "workflow_dispatch", "tag", "v0.1.0", "0.2.0", "0.1.0-SNAPSHOT", "0.1.0"),
    ("reject snapshot tag", "push", "tag", "v0.1.0-SNAPSHOT", "", "0.1.0-SNAPSHOT", None),
    ("reject main release", "push", "branch", "main", "", "0.1.0", None),
    ("reject invalid tag", "push", "tag", "vnext", "", "0.1.0-SNAPSHOT", None),
    ("reject invalid input", "workflow_dispatch", "branch", "main", "0.1", "0.1.0-SNAPSHOT", None),
    ("reject env injection", "workflow_dispatch", "branch", "main", "0.1.0\nINJECTED=true", "0.1.0-SNAPSHOT", None),
]

with tempfile.TemporaryDirectory() as temporary:
    directory = Path(temporary)
    output = directory / "github-env"
    for name, event, ref_type, ref, requested, base, expected in cases:
        (directory / "gradle.properties").write_text(f"VERSION_NAME={base}\n", newline="\n")
        output.write_text("")
        result = run_step("Resolve publication version", directory, {
            "REQUESTED_VERSION": requested, "GITHUB_EVENT_NAME": event,
            "GITHUB_REF_TYPE": ref_type, "GITHUB_REF_NAME": ref, "GITHUB_ENV": "github-env",
        })
        if expected is None:
            assert result.returncode != 0 and "::error::" in result.stdout, (name, result)
            assert output.read_text() == "", name
        else:
            assert result.returncode == 0, (name, result.stderr)
            assert output.read_text() == f"PUBLICATION_VERSION={expected}\n", name

    wrapper = directory / "gradlew"
    wrapper.write_text('#!/bin/bash\nprintf "%s\\n" "$@" > gradle-args\n', newline="\n")
    wrapper.chmod(0o755)
    credentials = {
        f"ORG_GRADLE_PROJECT_{key}": "test-only"
        for key in ("mavenCentralUsername", "mavenCentralPassword", "signingInMemoryKey")
    }
    for version, task in [("0.1.0-SNAPSHOT", "publishToMavenCentral"), ("0.2.0", "publishAndReleaseToMavenCentral")]:
        result = run_step("Publish all targets", directory, {**credentials, "PUBLICATION_VERSION": version})
        assert result.returncode == 0, result.stderr
        assert (directory / "gradle-args").read_text().splitlines() == [
            f":zenless-ui:{task}", f"-PVERSION_NAME={version}", "--stacktrace",
        ]
    for missing in credentials:
        (directory / "gradle-args").unlink(missing_ok=True)
        result = run_step("Publish all targets", directory, {
            **credentials, missing: "", "PUBLICATION_VERSION": "0.2.0",
        })
        assert result.returncode != 0 and not (directory / "gradle-args").exists(), missing

print(f"Passed {len(cases)} version cases, 2 publishing routes and 3 missing-credential checks.")
