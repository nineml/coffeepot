module namespace f = "https://coffeepot.nineml.org/ns/functions";

declare function f:choose-alternative(
  $alternatives as element()*
) as xs:integer {
  $alternatives[hex]/@alternative
};
