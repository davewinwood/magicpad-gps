# Build The APK With GitHub Actions

This route keeps your data use low because GitHub downloads the Android build tools on its own servers. Your connection only uploads this small project and downloads the finished APK.

## 1. Create A GitHub Repository

1. Go to https://github.com/new
2. Repository name: `magicpad-gps`
3. Visibility: private is fine.
4. Do not add a README, `.gitignore`, or licence on GitHub, because this folder already has project files.
5. Create the repository.

## 2. Upload This Project

From this folder, push the local repository to GitHub.

Replace `YOUR-USERNAME` with your GitHub username:

```powershell
git remote add origin https://github.com/YOUR-USERNAME/magicpad-gps.git
git branch -M main
git push -u origin main
```

GitHub may ask you to sign in. If it asks for a password, GitHub usually expects a personal access token rather than your account password.

If command-line sign-in is awkward, use the GitHub website upload option:

1. Open the new empty repository in your browser.
2. Choose **uploading an existing file**.
3. Drag in the contents of this folder, including:
   - `.github`
   - `app`
   - `docs`
   - `.gitignore`
   - `build.gradle`
   - `settings.gradle`
   - `README.md`
4. Do not upload the `.git` folder if Windows shows it.
5. Commit the upload.

If using the command line, run the `git remote`, `git branch`, and `git push` commands as your normal Windows user. The Codex sandbox could initialize `.git`, but it could not write the first commit because Windows reported the repository metadata as owned by a different local user.

## 3. Run The Build

1. Open the repository on GitHub.
2. Go to **Actions**.
3. Select **Android build**.
4. Choose **Run workflow**.
5. Wait for the build to finish.

## 4. Download The APK

1. Open the completed workflow run.
2. Scroll to **Artifacts**.
3. Download `magicpad-gps-debug-apk`.
4. Unzip it.
5. Install `app-debug.apk` on both the phone and the MagicPad.

## 5. If The Build Fails

Open the failed build, copy the error text, and paste it back into this chat. The first build may reveal small Android compile issues because this prototype has not yet been compiled on a machine with the Android SDK.
