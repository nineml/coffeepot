#!/bin/bash

if [ -z "$CIRCLE_TAG" ]; then
    echo "Only tagged commits are published"
else
    if [ -z "$GPGKEYURI" ]; then
        echo "Environment not configured for publishing"
    else
        curl -o secret.gpg $GPGKEYURI
        ./gradlew -PsonatypeUsername="$SONATYPEUSER" -PsonatypePassword="$SONATYPEPASS" \
                  -Psigning.keyId="$SIGNKEY" -Psigning.password="$SIGNPSW" \
                  -Psigning.secretKeyRingFile=./secret.gpg \
                  publish
        rm -f secret.gpg
    fi
fi

if [ "$CIRCLE_BRANCH" = "" ]; then
    # It appears that CircleCI doesn't set CIRCLE_BRANCH for tagged builds.
    # Assume we're doing them on the master branch, I guess.
    BRANCH=main
else
    BRANCH=$CIRCLE_BRANCH
fi

echo "Deploying website updates for $BRANCH branch"

if [ `git branch -r | grep "origin/gh-pages" | wc -l` = 0 ]; then
    echo "No gh-pages branch for publication"
    exit
fi

if [ -z "$GIT_EMAIL" -o -z "$GIT_USER" ]; then
    echo "No identity configured with GIT_USER/GIT_EMAIL"
    exit
fi

git config --global user.email $GIT_EMAIL
git config --global user.name $GIT_USER

# Save the website files
pushd build/website > /dev/null
tar cf - . | gzip > /tmp/dist.$$.tar.gz
popd > /dev/null

# Switch to the gh-pages branch
git checkout --track origin/gh-pages

# Delete the cruft not related to gh-pages
rm -rf website
git clean -d -f

# Unpack the website files
tar zxf /tmp/dist.$$.tar.gz
rm /tmp/dist.$$.tar.gz

git add --verbose .
git commit -m "Successful CircleCI build $CIRCLE_BUILD_NUM [ci skip]"
git push -fq origin gh-pages > /dev/null

echo "Published website to gh-pages."
