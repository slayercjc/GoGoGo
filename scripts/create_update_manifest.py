"""Create the app update contract from the actual signed APK, not hand-entered versions."""
import hashlib
import json
import pathlib
import re
import subprocess
import sys

apk = pathlib.Path(sys.argv[1])
aapt = sys.argv[2]
badging = subprocess.check_output([aapt, "dump", "badging", str(apk)], text=True)
package = re.search(r"package: name='([^']+)' versionCode='(\d+)' versionName='([^']+)'", badging)
sdk = re.search(r"sdkVersion:'(\d+)'", badging)
if not package or not sdk:
    raise SystemExit("Cannot read APK package/version/minSdk")
application_id, version_code, version_name = package.groups()
if application_id != "com.zcshou.gogogo" or not re.fullmatch(r"[0-9]+(?:\.[0-9]+){1,2}", version_name):
    raise SystemExit("Unexpected package or version name")
renamed = apk.with_name(f"HuanWei-{version_name}.apk")
if apk != renamed:
    apk.rename(renamed)
data = {
    "applicationId": application_id,
    "versionCode": int(version_code),
    "versionName": version_name,
    "minSdk": int(sdk.group(1)),
    "apk": renamed.name,
    "size": renamed.stat().st_size,
    "sha256": hashlib.sha256(renamed.read_bytes()).hexdigest(),
}
(renamed.parent / "update.json").write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
(renamed.parent / "SHA256SUMS.txt").write_text(f"{data['sha256']}  {renamed.name}\n", encoding="utf-8")
print(f"Created update.json for {version_name} ({version_code})")
