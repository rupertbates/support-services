package com.gu.support.promotions

import com.gu.i18n.Country
import com.gu.support.config.{PromotionsConfigProvider, Stages}
import com.gu.support.promotions.Fixtures.{freeTrialPromoCode, _}
import com.typesafe.config.ConfigFactory
import org.scalatest.{FlatSpec, Matchers}
import Fixtures._
import com.gu.i18n.Country.UK

//noinspection NameBooleanParameters
class PromotionServiceSpec extends FlatSpec with Matchers {

  val config = new PromotionsConfigProvider(ConfigFactory.load(), Stages.DEV).get()
  val serviceWithFixtures = new PromotionService(
    config,
    Some(new SimplePromotionCollection(List(freeTrial, discount, double, tracking, renewal)))
  )

  val serviceWithDynamo = new PromotionService(
    config,
    None
  )

  "PromotionService" should "find a PromoCode" in {
    serviceWithFixtures.findPromotion(freeTrialPromoCode) should be(Some(freeTrial))
  }

  it should "validate a PromoCode" in {
    serviceWithFixtures.validatePromoCode(freeTrialPromoCode, UK, validProductRatePlanId, false) should be(Right(ValidatedPromotion(freeTrialPromoCode, freeTrial)))
    serviceWithFixtures.validatePromoCode(invalidPromoCode, UK, validProductRatePlanId, false) should be(Left(NoSuchCode))
    serviceWithFixtures.validatePromoCode(freeTrialPromoCode, UK, invalidProductRatePlanId, false) should be(Left(InvalidProductRatePlan))
    serviceWithFixtures.validatePromoCode(freeTrialPromoCode, Country.US, validProductRatePlanId, false) should be(Left(InvalidCountry))
    serviceWithFixtures.validatePromoCode(renewalPromoCode, UK, validProductRatePlanId, false) should be(Left(NotApplicable))
  }

  it should "apply a discount PromoCode" in {
    val result = serviceWithFixtures.applyPromotion(discountPromoCode, UK, validProductRatePlanId, subscriptionData, false)
    result.ratePlanData.length shouldBe 2
    result.subscription.promoCode shouldBe Some(discountPromoCode)
  }

  it should "apply a FreeTrial PromoCode" in {
    val result = serviceWithFixtures.applyPromotion(freeTrialPromoCode, UK, validProductRatePlanId, subscriptionData, false)
    result.subscription.contractEffectiveDate shouldBe subscriptionData.subscription.contractEffectiveDate.plusDays(freeTrialBenefit.get.duration.getDays)
    result.subscription.promoCode shouldBe Some(freeTrialPromoCode)
  }

  it should "apply a double PromoCode" in {
    val result = serviceWithFixtures.applyPromotion(doublePromoCode, UK, validProductRatePlanId, subscriptionData, false)
    result.subscription.contractEffectiveDate shouldBe subscriptionData.subscription.contractEffectiveDate.plusDays(freeTrialBenefit.get.duration.getDays)
    result.ratePlanData.length shouldBe 2
    result.subscription.promoCode shouldBe Some(doublePromoCode)
  }

  it should "add any valid promo code to the subscription data" in {
    serviceWithFixtures
      .applyPromotion(trackingPromoCode, UK, validProductRatePlanId, subscriptionData, false)
      .subscription.promoCode shouldBe Some(trackingPromoCode)
  }

  it should "not apply an invalid PromoCode" in {
    serviceWithFixtures.applyPromotion(invalidPromoCode, UK, validProductRatePlanId, subscriptionData, false) shouldBe subscriptionData
  }

  it should "not apply a renewal code to a new subscription" in {
    serviceWithFixtures.applyPromotion(renewalPromoCode, UK, validProductRatePlanId, subscriptionData, false) shouldBe subscriptionData
  }

  it should "not apply a non renewal code to a renewal" in {
    serviceWithFixtures.applyPromotion(discountPromoCode, UK, validProductRatePlanId, subscriptionData, true) shouldBe subscriptionData
  }

  it should "apply a renewal code to a renewal" in {
    val result = serviceWithFixtures.applyPromotion(renewalPromoCode, UK, validProductRatePlanId, subscriptionData, true)
    result.ratePlanData.length shouldBe 2
    result.subscription.promoCode shouldBe Some(renewalPromoCode)
  }

  ignore should "apply a real promo code" in {
    val realPromoCode = "DJP8L27FY"
    val digipackMonthlyProductRatePlanId = "2c92c0f84bbfec8b014bc655f4852d9d"
    val result = serviceWithDynamo.applyPromotion(realPromoCode, UK, digipackMonthlyProductRatePlanId, subscriptionData, isRenewal = false)
    result.ratePlanData.length shouldBe 2
  }

}
