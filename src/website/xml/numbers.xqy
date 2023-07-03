module namespace f = "https://coffeepot.nineml.org/ns/functions";

declare function f:choose-alternative(
  $context as element(),
  $options as map(*)
) as map(*) {
  map {
    'selection': $context/children[symbol[@name='decimal']]/@id
  }
};
