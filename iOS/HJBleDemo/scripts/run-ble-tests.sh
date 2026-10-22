#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/.."
HJ_BLE_TEST_DIR="$(mktemp -d "${TMPDIR:-/tmp}/hjble-tests.XXXXXX")"
trap 'rm -rf "$HJ_BLE_TEST_DIR"' EXIT
HJ_BLE_DEVELOPER_DIR="$(xcode-select -p)"
HJ_BLE_TEST_FRAMEWORKS="$HJ_BLE_DEVELOPER_DIR/Platforms/MacOSX.platform/Developer/Library/Frameworks"
HJ_BLE_POD_DIR="$PWD/Pods/wiseBle/WiseBle"
if [[ ! -f "$HJ_BLE_POD_DIR/WWBluetoothLE.m" ]]; then
    echo '请先在 iOS/HJBleDemo 目录执行 pod install。' >&2
    exit 1
fi
mkdir -p "$HJ_BLE_TEST_DIR/HJBleRegressions.xctest/Contents/MacOS" "$HJ_BLE_TEST_DIR/include/wiseBle"
cp "$HJ_BLE_POD_DIR"/*.h "$HJ_BLE_TEST_DIR/include/wiseBle/"
cat > "$HJ_BLE_TEST_DIR/HJBleRegressions.xctest/Contents/Info.plist" <<'PLIST'
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0"><dict>
<key>CFBundleIdentifier</key><string>com.hongjia.HJBleRegressions</string>
<key>CFBundleExecutable</key><string>HJBleRegressions</string>
<key>CFBundlePackageType</key><string>BNDL</string>
</dict></plist>
PLIST
xcrun --sdk macosx clang -fobjc-arc -fmodules -bundle \
    -I "$HJ_BLE_TEST_DIR/include" -I "$HJ_BLE_POD_DIR" -I "$HJ_BLE_POD_DIR/category" \
    -I HJBleDemo/Storage -I HJBleDemo/Home/model -F "$HJ_BLE_TEST_FRAMEWORKS" \
    -framework Foundation -framework CoreBluetooth -framework XCTest -lsqlite3 \
    -Wl,-rpath,"$HJ_BLE_TEST_FRAMEWORKS" \
    "$HJ_BLE_POD_DIR"/*.m "$HJ_BLE_POD_DIR"/category/*.m \
    HJBleDemo/Home/model/HJBleDevicePageLifecycle.m HJBleDemo/Storage/*.m \
    HJBleDemo/Home/model/HJCustomCommand.m HJBleDemo/Home/model/HJCustomCommandStore.m HJBleDemoTests/*.m \
    -o "$HJ_BLE_TEST_DIR/HJBleRegressions.xctest/Contents/MacOS/HJBleRegressions"
"$HJ_BLE_DEVELOPER_DIR/Platforms/MacOSX.platform/Developer/Library/Xcode/Agents/xctest" \
    "$HJ_BLE_TEST_DIR/HJBleRegressions.xctest"
