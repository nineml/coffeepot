(function() {
  const p = document.querySelector("#countdown");
  let timer = 10;
  
  const countdown = function() {
    if (!p) {
      return;
    }

    if (timer === 0) {
      p.innerHTML = " ";
    } else {
      p.innerHTML = timer;
    }

    if (timer > 0) {
      timer -= 1;
      setTimeout(countdown, 1000);
    }
  };

  countdown();
})();
