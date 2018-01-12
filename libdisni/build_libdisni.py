#!/usr/bin/env python
import errno
import glob
import os
import shutil
import subprocess
import sys

def maybe_makedirs(path):
    path = normpath(path)
    print("mkdir -p " + path)
    try:
        os.makedirs(path)
    except OSError as e:
        if e.errno != errno.EEXIST:
            raise


def run(command, **kwargs):
    print(command)
    subprocess.check_call(command, shell=True, **kwargs)


def cp(source, target):
    source = normpath(source)
    target = normpath(target)
    print("cp {0} {1}".format(source, target))
    shutil.copy(source, target)


def normpath(path):
    """Normalize UNIX path to a native path."""
    normalized = os.path.join(*path.split("/"))
    if os.path.isabs(path):
        return os.path.abspath("/") + normalized
    else:
        return normalized


if __name__ == "__main__":
    print("building libdisni")
    maybe_makedirs("build")
    run("./autoprepare.sh")
    run("./configure --with-jdk={} --prefix={} {}".format(os.environ["JAVA_HOME"], 
							  os.path.join(os.getcwd(), "build"), 
							  sys.argv[1] if len(sys.argv) == 2 else ""))
    run("make")
    run("make install")
    print("copying native library")
    maybe_makedirs("../src/main/resources/lib")
    cp("build/lib/libdisni.so", "../src/main/resources/lib")
