with open("app/build.gradle.kts", "r") as f:
    content = f.read()

target = "implementation(libs.googleid)"
replacement = 'implementation(libs.googleid)\n  implementation("com.google.android.gms:play-services-auth:21.3.0")'

if target in content and "play-services-auth" not in content:
    content = content.replace(target, replacement)
    with open("app/build.gradle.kts", "w") as f:
        f.write(content)
    print("Updated build.gradle.kts successfully")
else:
    print("Already updated or target not found")
