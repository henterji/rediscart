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
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.promotionengineservices.model.RuleBasedOrderAdjustTotalActionModel;
import de.hybris.platform.promotions.model.PromotionResultModel;
import de.hybris.platform.ruleengineservices.rao.AbstractRuleActionRAO;
import de.hybris.platform.ruleengineservices.rao.DiscountRAO;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Encapsulates logic of Rule Action processing for Cart total calculation.
 *
 * @author Henter Liu (henterji@163.com)
 */
public class CustomizedOrderAdjustTotalActionStrategy extends CustomizedRuleActionStrategy<RuleBasedOrderAdjustTotalActionModel>
{
	private static final Logger LOG = LoggerFactory.getLogger(CustomizedOrderAdjustTotalActionStrategy.class);

	/**
	 * Creates a {@code PromotionResultModel} if the parameter action is of type {@link DiscountRAO} the discount is added
	 * to corresponding to the action Order model.
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
		final PromotionResultModel promoResult = getPromotionActionService().createPromotionResult(action);
		if (promoResult == null)
		{
			LOG.error("cannot apply {}, promotionResult could not be created.", this.getClass().getSimpleName());
			return Collections.emptyList();
		}

		final AbstractOrderModel order = promoResult.getOrder();
		if (order == null)
		{
			LOG.error("cannot apply {}, order not found", this.getClass().getSimpleName());
			// detach the promotion result if its not saved yet.
			if (getModelService().isNew(promoResult))
			{
				getModelService().detach(promoResult);
			}
			return Collections.emptyList();
		}

		final DiscountRAO discountRao = (DiscountRAO) action;
		final RuleBasedOrderAdjustTotalActionModel actionModel = createOrderAdjustTotalAction(promoResult, discountRao);
		handleActionMetadata(action, actionModel);

		getPromotionActionService().createDiscountValue(discountRao, actionModel.getGuid(), order);

		getModelService().saveAll(promoResult, actionModel, order);

		recalculateIfNeeded(order);

		return Collections.singletonList(promoResult);
	}

	protected RuleBasedOrderAdjustTotalActionModel createOrderAdjustTotalAction(final PromotionResultModel promoResult,
			final DiscountRAO discountRao)
	{
		final RuleBasedOrderAdjustTotalActionModel actionModel = createPromotionAction(promoResult, discountRao);
		actionModel.setAmount(discountRao.getValue());
		return actionModel;
	}

	@Override
	public void undo(final ItemModel action)
	{
		if (action instanceof RuleBasedOrderAdjustTotalActionModel)
		{
			handleUndoActionMetadata((RuleBasedOrderAdjustTotalActionModel) action);
			final AbstractOrderModel order = undoInternal((RuleBasedOrderAdjustTotalActionModel) action);
			recalculateIfNeeded(order);
		}
	}
}
