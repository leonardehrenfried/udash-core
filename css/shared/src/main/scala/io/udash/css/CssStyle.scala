package io.udash.css

import scalacss.internal.{FontFace, StyleS}

/** Representation of stylesheet elements. In JS it's always `CssStyleName`. */
sealed trait CssStyle {
  def className: String
  // Primarily introduced for FontAwesome support, which requires adding two classes, e.g. "fa fa-adjust"
  def commonPrefixClass: Option[String] = None
  final def classNames: Seq[String] = commonPrefixClass.toList :+ className
}
case class CssStyleName(className: String) extends CssStyle
case class CssPrefixedStyleName(prefixClass: String, actualClassSuffix: String) extends CssStyle {
  val className = s"$prefixClass-$actualClassSuffix"
  override val commonPrefixClass: Option[String] = Some(prefixClass)
}
case class CssStyleImpl(className: String, impl: StyleS) extends CssStyle
case class CssKeyframes(className: String, steps: Map[Double, StyleS]) extends CssStyle
case class CssFontFace(className: String, font: FontFace[Option[String]]) extends CssStyle
