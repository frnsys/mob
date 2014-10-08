#activator dist

archive=mob-1.0-SNAPSHOT
scp target/universal/$archive.zip $1:/srv/mob/
ssh -t $1 bash -c "'
    cd /srv/mob/;
    sudo rm -rf mob;
    unzip -x $archive.zip;
    mv $archive mob;
    sudo service supervisor restart
'"