/*
 * Copyright (c) 2017, Salesforce.com, Inc.
 * All rights reserved.
 */

package com.salesforce.op.stages.impl.feature

import com.salesforce.op._
import com.salesforce.op.features.types._
import com.salesforce.op.test.{TestFeatureBuilder, TestSparkContext}
import com.salesforce.op.utils.spark.RichDataset._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Assertions, FlatSpec, Matchers}


@RunWith(classOf[JUnitRunner])
class FilterMapTest extends FlatSpec with TestSparkContext {

  val (ds, f1) = TestFeatureBuilder[TextMap](
    Seq(
      TextMap(Map("Arthur" -> "King", "Lancelot" -> "Brave", "Galahad" -> "Pure")),
      TextMap(Map("Lancelot" -> "Brave", "Galahad" -> "Pure", "Bedevere" -> "Wise")),
      TextMap(Map("Knight" -> "Ni"))
    )
  )

  val filter = new FilterMap[TextMap]().setInput(f1)


  val (dsInt, f1Int) = TestFeatureBuilder[IntegralMap](
    Seq(
      IntegralMap(Map("Arthur" -> 1, "Lancelot" -> 2, "Galahad" -> 3)),
      IntegralMap(Map("Lancelot" -> 2, "Galahad" -> 3, "Bedevere" -> 4)),
      IntegralMap(Map("Knight" -> 5))
    )
  )
  val filterInt = new FilterMap[IntegralMap]().setInput(f1Int)


  val (dsCat, f1Cat) = TestFeatureBuilder[MultiPickListMap](
    Seq(
      MultiPickListMap(Map("Arthur" -> Set("King", "Briton"),
        "Lancelot" -> Set("Brave", "Knight"),
        "Galahad" -> Set("Pure", "Knight"))),
      MultiPickListMap(Map("Lancelot" -> Set("Brave", "Knight"),
        "Galahad" -> Set("Pure", "Knight"),
        "Bedevere" -> Set("Wise", "Knight"))),
      MultiPickListMap(Map("Knight" -> Set("Ni", "Ekke Ekke Ekke Ekke Ptang Zoo Boing")))
    )
  )
  val filterCat = new FilterMap[MultiPickListMap]().setInput(f1Cat)


  classOf[FilterMap[_]].getSimpleName should "return single properly formed feature" in {
    val filtered = filter.getOutput()

    filtered.name shouldBe filter.outputName
    filtered.originStage shouldBe filter
    filtered.parents shouldBe Array(f1)
  }

  it should "filter TextMap by whitelisted keys" in {
    filter.setWhiteListKeys(Array("Arthur", "Knight"))

    val filtered = filter.transform(ds).collect(filter.getOutput)
    val dataExpected = Array(
      TextMap(Map("Arthur" -> "King")),
      TextMap.empty,
      TextMap(Map("Knight" -> "Ni"))
    )

    filtered should contain theSameElementsAs dataExpected
  }

  it should "filter TextMap by blacklisted keys" in {
    filter.setInput(f1)
      .setWhiteListKeys(Array[String]())
      .setBlackListKeys(Array("Arthur", "Knight"))
    val filtered = filter.transform(ds).collect(filter.getOutput)

    val dataExpected = Array(
      TextMap(Map("Lancelot" -> "Brave", "Galahad" -> "Pure")),
      TextMap(Map("Lancelot" -> "Brave", "Galahad" -> "Pure", "Bedevere" -> "Wise")),
      TextMap.empty
    )

    filtered should contain theSameElementsAs dataExpected
  }

  it should "filter IntegralMap by whitelisted keys" in {
    filterInt.setWhiteListKeys(Array("Arthur", "Knight"))
    val filtered = filterInt.transform(dsInt).collect(filterInt.getOutput())

    val dataExpected = Array(
      IntegralMap(Map("Arthur" -> 1)),
      IntegralMap.empty,
      IntegralMap(Map("Knight" -> 5))
    )

    filtered should contain theSameElementsAs dataExpected
  }

  it should "filter IntegralMap by blacklisted keys" in {
    filterInt.setInput(f1Int)
      .setWhiteListKeys(Array[String]())
      .setBlackListKeys(Array("Arthur", "Knight"))
    val filtered = filterInt.transform(dsInt).collect(filterInt.getOutput())

    val dataExpected = Array(
      IntegralMap(Map("Lancelot" -> 2, "Galahad" -> 3)),
      IntegralMap(Map("Lancelot" -> 2, "Galahad" -> 3, "Bedevere" -> 4)),
      IntegralMap.empty
    )

    filtered should contain theSameElementsAs dataExpected
  }

  it should "filter MultiPickListMap by whitelisted keys" in {
    filterCat.setWhiteListKeys(Array("Arthur", "Knight"))
    val filtered = filterCat.transform(dsCat).collect(filterCat.getOutput())

    val dataExpected = Array(
      MultiPickListMap(Map("Arthur" -> Set("King", "Briton"))),
      MultiPickListMap.empty,
      MultiPickListMap(Map("Knight" -> Set("Ni", "EkkeEkkeEkkeEkkePtangZooBoing")))
    )

    filtered should contain theSameElementsAs dataExpected
  }

  it should "filter MultiPickListMap by blacklisted keys" in {
    filterCat
      .setWhiteListKeys(Array[String]())
      .setBlackListKeys(Array("Arthur", "Knight"))

    val filtered = filterCat.transform(dsCat).collect(filterCat.getOutput())

    val dataExpected = Array(
      MultiPickListMap(Map("Lancelot" -> Set("Brave", "Knight"),
        "Galahad" -> Set("Pure", "Knight"))),
      MultiPickListMap(Map("Lancelot" -> Set("Brave", "Knight"),
        "Galahad" -> Set("Pure", "Knight"),
        "Bedevere" -> Set("Wise", "Knight"))),
      MultiPickListMap.empty
    )

    filtered should contain theSameElementsAs dataExpected
  }

  it should "filter correctly when using shortcut" in {
    val filtered = f1.filter(whiteList = Seq("Arthur", "Knight"), blackList = Seq())

    filtered.name shouldBe filtered.originStage.outputName
    filtered.originStage shouldBe a[FilterMap[_]]
    filtered.parents shouldBe Array(f1)
  }

  it should "set cleanMapFlag correctly" in {
    filter.setCleanText(false)
    filter.get[Boolean](filter.cleanText).get shouldBe false
    filter.setCleanKeys(false)
    filter.get[Boolean](filter.cleanKeys).get shouldBe false
  }

  it should "not clean map when flag set to false" in {
    filterCat
      .setCleanText(false)
      .setCleanKeys(false)
      .setWhiteListKeys(Array("Arthur", "Knight"))
      .setBlackListKeys(Array())
    val filtered = filterCat.transform(dsCat).collect(filterCat.getOutput())

    val dataExpected = Array(
      MultiPickListMap(Map("Arthur" -> Set("King", "Briton"))),
      MultiPickListMap.empty,
      MultiPickListMap(Map("Knight" -> Set("Ni", "Ekke Ekke Ekke Ekke Ptang Zoo Boing")))
    )
    filtered should contain theSameElementsAs dataExpected
  }

  it should "clean map when flag set to true" in {
    filterCat
      .setCleanKeys(true)
      .setCleanText(true)
      .setWhiteListKeys(Array("Arthur", "Knight"))
      .setBlackListKeys(Array())
    val filtered = filterCat.transform(dsCat).collect(filterCat.getOutput())

    val dataExpected = Array(
      MultiPickListMap(Map("Arthur" -> Set("King", "Briton"))),
      MultiPickListMap.empty,
      MultiPickListMap(Map("Knight" -> Set("Ni", "EkkeEkkeEkkeEkkePtangZooBoing")))
    )
    filtered should contain theSameElementsAs dataExpected
  }

}