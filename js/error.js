(function() {
  const p = document.querySelector("p");
  let href = window.location.href;

  if (!href.startsWith("http:") && !href.startsWith("https:")) {
    return;
  }

  fetch("/versions.json")
    .then(response => response.json())
    .then(data => {
      const versions = data.versions;

/*
      let pos = href.indexOf("//");
      let scheme = href.substring(0, pos+2);
      href = href.substring(pos+2);

      pos = href.indexOf("/");
      if (pos < 0) {
        return;
      }

      let hostname = href.substring(0, pos);
      href = href.substring(pos+1);

      pos = href.indexOf("/");
      let base = "";
      if (pos > 0) {
        base = href.substring(0, pos);
      }

      console.log(`base: ${base}, href: ${href}`);

      let redirect = "";
      if (base === "") {
        redirect = `${scheme}${hostname}/${versions.currentRelease}/${href}`;
        window.location.href = redirect;
      }

      if (base === versions.currentRelease) {
        redirect = `${scheme}${hostname}/${versions.ninemlVersion}/${href}`;
        p.innerHTML = `Can't find it in the current release, perhaps in ${redirect}`;
        return;
      }

      if (base === versions.ninemlVersion) {
        redirect = `${scheme}${hostname}/${versions.currentRelease}/${href}`;
        p.innerHTML = `Can't find it in the beta release, perhaps in ${redirect}`;
        return;
      }

      redirect = `${scheme}${hostname}/${versions.currentRelease}/${href}`;

      console.log("BASE: " + base);
      if (base.match(/^[0-9]/)) {
        window.location.href = redirect;
        return;
      }
*/

      p.innerHTML = `Please <a href='https://github.com/nineml/nineml/issues'>open an issue</a> if you think this is an error on the site.`;
    });
})();
