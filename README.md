# Gradle-Import-Aar

[![Build Status](https://travis-ci.com/nekocode/Gradle-Import-Aar.svg?branch=master)](https://travis-ci.com/nekocode/Gradle-Import-Aar) [![codecov](https://codecov.io/gh/nekocode/Gradle-Import-Aar/branch/master/graph/badge.svg)](https://codecov.io/gh/nekocode/Gradle-Import-Aar)

With this plugin, you can import AARs (android libraries) in a pure java gradle project, so that you can reference classes in them.

## Usage

The `${lastest-version}` of this plugin is [![Release](https://jitpack.io/v/nekocode/Gradle-Import-Aar.svg)](https://jitpack.io/#nekocode/Gradle-Import-Aar). Copy below code to the build.gradle of your java project:

```gradle
buildscript {
    repositories {
        maven { url 'https://jitpack.io' }
    }
    dependencies {
        classpath "com.github.nekocode:Gradle-Import-Aar:{lastest-version}"
    }
}

apply plugin: 'import-aar'
```

Then the plugin will list your original configurations, and create corresponding new configurations naming `${originalName}Aar` (Such as `compileOnlyAar`) for importing AAR dependencies from maven. Take an example:

```gradle
dependencies {
    compileOnlyAar "com.android.support:appcompat-v7:27.0.2"
}
```

There is a demo module [pureJavaLib](pureJavaLib) in this porject, you can check it to learn more details.
