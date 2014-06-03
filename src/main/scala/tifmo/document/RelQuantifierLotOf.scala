package tifmo.document

import tifmo.dcstree.Relation

case object RelQuantifierLotOf
  extends Relation
  with RelLeftUpEntailing
  with RelRightUpEntailing
  with RelWithNonEmptyIntersection
  with RelConservative
