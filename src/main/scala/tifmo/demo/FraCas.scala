package tifmo.demo

import tifmo.inference.{IEPredRL, IEPredSubsume, RuleDo, IEngine}
import tifmo.main.en.{parse, EnWord}
import mylib.res.en.EnWordNet
import tifmo.document.RelPartialOrder

object FraCas {
	def main(args: Array[String]) {
		if (args.length != 1) {
			println("USAGE: tifmo.demo.FraCaS fracas_xml")
			sys.exit()
		}

		val fracas_xml = args(0)
		val f = xml.XML.loadFile(fracas_xml)

		for (p <- f \ "problem") {

			val id = (p \ "@id").text

			val sm =
				if ((p \ "p").length == 1) "single"
				else "multi"

			val fracas_answer = (p \ "@fracas_answer").text

			if (fracas_answer == "undef") {
				println("%s,%s,undef,ignore".format(id, sm))
			} else {
				val t = (p \ "p").map(_.text.trim).mkString(" ")
				val h = (p \ "h").text.trim

				val (tdoc, hdoc) = parse(t, h)

				val words = tdoc.allContentWords[EnWord] ++ hdoc.allContentWords[EnWord]
				def sameWordSynonym(x: IEngine) {
					for (s <- words.subsets(2)) {
						val a = s.head
						val b = (s - a).head
						if (EnWordNet.stem(a.lemma, a.mypos) == EnWordNet.stem(b.lemma, b.mypos)) {
							x.subsume(a, b)
							x.subsume(b, a)
						}
					}
				}

				val prem = tdoc.makeDeclaratives.flatMap(_.toStatements)
				val hypo = hdoc.makeDeclaratives.flatMap(_.toStatements)

				val ie = new IEngine()

				prem.foreach(ie.claimStatement)
				hypo.foreach(ie.checkStatement)

				sameWordSynonym(ie)

				// Adding semantic knowledge for handling fracas-220
				// TODO Refactor so that common semantic knowledge is re-usable
				for (
					r@RelPartialOrder(lemma) <- tdoc.allRelations;
					deno <- ie.allDenotationWordSign.find(_.word.asInstanceOf[EnWord].lemma == lemma)
				) {
					val term = ie.getTerm(deno)
					ie.foreachSubset(term.index, Seq.empty, RuleDo((ie, pred, arg) => {
						pred match {
							case IEPredSubsume(sub, _) =>
								ie.foreachARLX(sub, Seq.empty, RuleDo((ie, rlPred, arg) => {
									rlPred match {
										case IEPredRL(`sub`, `r`, b) =>
											ie.claimSubsume(b, term.index)
									}
								}))
						}
					}))
				}

				ie.explore()

				val answer =
					if (hypo.forall(ie.checkStatement)) {
						"yes"
					} else {
						// negate hdoc
						for (n <- hdoc.allRootNodes) {
							n.rootNeg = !n.rootNeg
						}

						val nhypo = hdoc.makeDeclaratives.flatMap(_.toStatements)
						val nie = new IEngine

						prem.foreach(nie.claimStatement)
						nhypo.foreach(nie.checkStatement)

						if (nhypo.forall(nie.checkStatement)) {
							"no"
						} else {
							"unknown"
						}
					}

				println("%s,%s,%s,%s".format(id, sm, fracas_answer, answer))
				if (fracas_answer != answer) {
					System.err.println("T: " + t)
					System.err.println("H: " + h)
				}
			}
		}
	}
}
