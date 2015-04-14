./gradlew uploadArchives
cp -rf ./build/maven/ $CIRCLE_ARTIFACTS/maven/
git clone "https://github.com/kr9ly/maven" /tmp/maven
cd /tmp/maven
git checkout gh-pages
cp -rf ${HOME}/salmon/build/maven/net /tmp/maven
if [[ $(git status --porcelain) ]];
then
	echo "start deploying.";
    git add -A .
    git config user.name ${GIT_USER_NAME}
    git config user.email ${GIT_USER_EMAIL}
    git commit -m 'Commit from CircleCI'
    git push origin gh-pages
else
    echo "nothing to commit.";
fi