# JaKsB

JaKsB is a library to ... via ksp(Kotlin Symbol Processing).

Apply the ksp plugin and add the dependency in the module containing the JaxB classes:

```groovy
plugins {
    id 'com.google.devtools.ksp' version '2.0.21-1.0.26'
}
```

```groovy
ksp 'io.github.guicamest.jaksb:processor:{latest-version}'
```

## Usage
TODO

## Development

_Requires Java >= 11_

### Install jdk using SDKMAN!

If you are using [SDKMAN!](https://sdkman.io/) to manage your sdks, you can add a `.sdkmanrc` file to the root
(already added to .gitignore) with the following content:
```properties
java=11.0.20.1-tem # Version may mismatch, adjust to the sdk that you have installed
#java=17.0.7-tem # Version may mismatch, adjust to the sdk that you have installed
#java=21.0.1-tem # Version may mismatch, adjust to the sdk that you have installed

# You can also enable auto-env setting `sdkman_auto_env=true` config
# in [~/.sdkman/etc/config]. See https://sdkman.io/usage#config
```

And then simply run
```shell
sdk env install
```

## Contributing

Bug reports, feature requests and pull requests are welcome on GitHub at <https://github.com/guicamest/jaksb>.
