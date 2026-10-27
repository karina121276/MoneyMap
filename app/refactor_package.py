import os
import shutil

old_pkg = "com.example"
new_pkg = "com.thecorporatedeveloper.moneymap"

old_app_id = "com.aistudio.moneymap.app"
new_app_id = "com.thecorporatedeveloper.moneymap"

print("Starting package refactoring from", old_pkg, "to", new_pkg)

# 1. Update build.gradle.kts
gradle_path = "app/build.gradle.kts"
if os.path.exists(gradle_path):
    with open(gradle_path, "r") as f:
        content = f.read()
    content = content.replace('namespace = "com.example"', f'namespace = "{new_pkg}"')
    content = content.replace(f'applicationId = "{old_app_id}"', f'applicationId = "{new_app_id}"')
    content = content.replace('com.aistudio.moneymap.app', new_app_id)
    with open(gradle_path, "w") as f:
        f.write(content)
    print("Updated build.gradle.kts")

# 2. Update AndroidManifest.xml
manifest_path = "app/src/main/AndroidManifest.xml"
if os.path.exists(manifest_path):
    with open(manifest_path, "r") as f:
        content = f.read()
    content = content.replace("com.example.ACTION_MONEYMAP_DATA_CHANGED", f"{new_pkg}.ACTION_MONEYMAP_DATA_CHANGED")
    content = content.replace(old_app_id, new_app_id)
    content = content.replace("com.example", new_pkg)
    with open(manifest_path, "w") as f:
        f.write(content)
    print("Updated AndroidManifest.xml")

# 3. Process all source, test, androidTest files
for root_dir in ["app/src/main/java", "app/src/test/java", "app/src/androidTest/java"]:
    if not os.path.exists(root_dir):
        continue
    for dirpath, dirnames, filenames in os.walk(root_dir):
        for filename in filenames:
            if filename.endswith(".kt") or filename.endswith(".java") or filename.endswith(".xml"):
                filepath = os.path.join(dirpath, filename)
                with open(filepath, "r", encoding="utf-8", errors="ignore") as f:
                    text = f.read()
                
                # Replace package/imports/references
                new_text = text.replace("package com.example", f"package {new_pkg}")
                # handle subpackages e.g. package com.example.ui -> package com.thecorporatedeveloper.moneymap.ui
                new_text = new_text.replace("com.example.", f"{new_pkg}.")
                new_text = new_text.replace("com.example", new_pkg)
                new_text = new_text.replace(old_app_id, new_app_id)
                
                if new_text != text:
                    with open(filepath, "w", encoding="utf-8") as f:
                        f.write(new_text)
                    print(f"Updated references in {filepath}")

# 4. Move directory structures
for src_base, test_sub in [
    ("app/src/main/java/com/example", "app/src/main/java/com/thecorporatedeveloper/moneymap"),
    ("app/src/test/java/com/example", "app/src/test/java/com/thecorporatedeveloper/moneymap"),
    ("app/src/androidTest/java/com/example", "app/src/androidTest/java/com/thecorporatedeveloper/moneymap")
]:
    if os.path.exists(src_base):
        os.makedirs(test_sub, exist_ok=True)
        for item in os.listdir(src_base):
            s = os.path.join(src_base, item)
            d = os.path.join(test_sub, item)
            if os.path.isdir(s):
                if os.path.exists(d):
                    # merge directories
                    for subitem in os.listdir(s):
                        shutil.move(os.path.join(s, subitem), os.path.join(d, subitem))
                    shutil.rmtree(s)
                else:
                    shutil.move(s, d)
            else:
                shutil.move(s, d)
        print(f"Moved contents from {src_base} to {test_sub}")

# Clean up empty old com/example dirs
for base in ["app/src/main/java/com/example", "app/src/test/java/com/example", "app/src/androidTest/java/com/example"]:
    if os.path.exists(base):
        shutil.rmtree(base, ignoreCaller=True) rescue None

print("Refactoring script completed successfully.")
