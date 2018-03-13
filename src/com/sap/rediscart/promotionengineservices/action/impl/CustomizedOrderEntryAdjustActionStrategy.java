/*
 * Copyright [2018] [Henter Liu]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sap.rediscart.promotionengineservices.action.impl;

import de.hybris.platform.core.model.ItemModel;
import de.hybris.platform.core.model.order.AbstractOrderEntryModel;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.promotionengineservices.model.RuleBasedOrderEntryAdjustActionModel;
import de.hybris.platform.promotions.model.PromotionResultModel;
import de.hybris.platform.ruleengineservices.rao.AbstractRuleActionRAO;
import de.hybris.platform.ruleengineservices.rao.DiscountRAO;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Encapsulates logic of Cart total calculation after Line Item Discount as a Promotion Rule is applied.
 *
 * @author Henter Liu (henterji@163.com)
 */
public class CustomizedOrderEntryAdjustActionStrategy extends CustomizedRuleActionStrategy<RuleBasedOrderEntryAdjustActionModel>
{
	private static final Logger LOG = LoggerFactory.getLogger(CustomizedOrderEntryAdjustActionStrategy.class);

	/**
	 * If the parameter action is of type {@link DiscountRAO} and is applied to Order Entry the discount is added to
	 * corresponding Order Entry model.
	 *
	 * @return list of {@link PromotionResultModel} as a result of the {@link DiscountRAO} application.
	 */
	@Override
	public List<PromotionResultModel> apply(final AbstractRuleActionRAO action)
	{
		if (!(action instanceof DiscountRAO))
		{
			LOG.error("cannot apply {}, action is not of type DiscountRAO", this.getClass().getSimpleName());
			return Collections.emptyList();
		}

		// the action.getAppliedToObject() must contain an OrderEntryRAO
		final AbstractOrderEntryModel entry = getPromotionActionService().getOrderEntry(action);
		if (entry == null)
		{
			LOG.error("cannot apply {}, orderEntry could not be found.", this.getClass().getSimpleName());
			return Collections.emptyList();
		}

		final PromotionResultModel promoResult = getPromotionActionService().createPromotionResult(action);
		if (promoResult == null)
		{
			LOG.error("cannot apply {}, promotionResult could not be created.", this.getClass().getSimpleName());
			return Collections.emptyList();
		}

		final AbstractOrderModel order = entry.getOrder();
		if (order == null)
		{
			LOG.error("cannot apply {}, order does not exist for order entry", this.getClass().getSimpleName());
			// detach the promotion result if its not saved yet.
			if (getModelService().isNew(promoResult))
			{
				getModelService().detach(promoResult);
			}
			return Collections.emptyList();
		}

		final DiscountRAO discountRao = (DiscountRAO) action;
		final BigDecimal discountAmount = discountRao.getValue();

		adjustDiscountRaoValue(entry, discountRao, discountAmount);

		final RuleBasedOrderEntryAdjustActionModel actionModel = createOrderEntryAdjustAction(promoResult, action, entry,
				discountAmount);
		handleActionMetadata(action, actionModel);

		getPromotionActionService().createDiscountValue(discountRao, actionModel.getGuid(), entry);

		getModelService().saveAll(promoResult, actionModel, order, entry);

		recalculateIfNeeded(order);
		return Collections.singletonList(promoResult);
	}

	protected void adjustDiscountRaoValue(final AbstractOrderEntryModel entry, final DiscountRAO discountRao,
			final BigDecimal discountAmount)
	{
		// DiscountValue is always per unit and discount in DiscountRao is per unit, but for applicable units (appliedToQuantity)
		if ((!StringUtils.isEmpty(discountRao.getCurrencyIsoCode()) && discountRao.getAppliedToQuantity() > 0)
				|| discountRao.isPerUnit())
		{
			final BigDecimal amount = (discountAmount.multiply(BigDecimal.valueOf(discountRao.getAppliedToQuantity())))
					.divide(BigDecimal.valueOf(entry.getQuantity().longValue()), 5, BigDecimal.ROUND_HALF_UP);
			discountRao.setValue(amount);
		}
	}

	protected RuleBasedOrderEntryAdjustActionModel createOrderEntryAdjustAction(final PromotionResultModel promoResult,
			final AbstractRuleActionRAO action, final AbstractOrderEntryModel entry, final BigDecimal discountAmount)
	{
		final RuleBasedOrderEntryAdjustActionModel actionModel = createPromotionAction(promoResult, action);
		actionModel.setAmount(discountAmount);
		actionModel.setOrderEntryNumber(entry.getEntryNumber());
		actionModel.setOrderEntryProduct(entry.getProduct());
		actionModel.setOrderEntryQuantity(Long.valueOf(getConsumedQuantity(promoResult)));
		return actionModel;
	}

	@Override
	public void undo(final ItemModel action)
	{
		if (action instanceof RuleBasedOrderEntryAdjustActionModel)
		{
			handleUndoActionMetadata((RuleBasedOrderEntryAdjustActionModel) action);
			final AbstractOrderModel order = undoInternal((RuleBasedOrderEntryAdjustActionModel) action);
			recalculateIfNeeded(order);
		}
	}

	/**
	 * Sums up quantities of all consumed entries of given order entry.
	 *
	 * @param promoResult
	 *           AbstractOrderEntryModel to find consumed quantity for
	 * @return consumed quantity of given order entry
	 */
	protected long getConsumedQuantity(final PromotionResultModel promoResult)
	{
		long consumedQuantity = 0;
		if (CollectionUtils.isNotEmpty(promoResult.getConsumedEntries()))
		{
			consumedQuantity = promoResult.getConsumedEntries().stream()
					.mapToLong(consumedEntry -> consumedEntry.getQuantity().longValue()).sum();
		}
		return consumedQuantity;
	}
}
