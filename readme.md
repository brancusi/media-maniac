<img src="logo.png" width="30%" alt="Polylith" id="logo">

The Polylith documentation can be found here:

- The [high-level documentation](https://polylith.gitbook.io/polylith)
- The [poly tool documentation](https://cljdoc.org/d/polylith/clj-poly/CURRENT)
- The [RealWorld example app documentation](https://github.com/furkan3ayraktar/clojure-polylith-realworld-example-app)

You can also get in touch with the Polylith Team on [Slack](https://clojurians.slack.com/archives/C013B7MQHJQ).

<h1>media-maniac</h1>

### Setup

1. Install xxhash

```
brew install xxhash
```

2. Install container manager on synology nas
3. Enable ssh on the NAS
4. Generate a key set on OSX

```
ssh-keygen -t ed25519 -C "something@user"
```

5. Save the key to something like id_ed25519_nas
6. Install the pub key on the nas at: ~/.ssh/authorized_keys
