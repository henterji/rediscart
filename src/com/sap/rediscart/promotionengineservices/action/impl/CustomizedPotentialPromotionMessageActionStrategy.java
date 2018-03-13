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
import de.hybris.platform.promotionengineservices.model.PromotionActionParameterModel;
import de.hybris.platform.promotionengineservices.model.RuleBasedPotentialPromotionMessageActionModel;
import de.hybris.platform.promotions.model.PromotionResultModel;
import de.hybris.platform.ruleengineservices.rao.AbstractRuleActionRAO;
import de.hybris.platform.ruleengineservices.rao.DisplayMessageRAO;

import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Encapsulates logic of displaying potential promotion message.
 *
 * @author Henter Liu (henterji@163.com)
 */
public class CustomizedPotentialPromotionMessageActionStrategy
		extends CustomizedRuleActionStrategy<RuleBasedPotentialPromotionMessageActionModel>
{
	private static final Logger LOG = LoggerFactory.getLogger(CustomizedPotentialPromotionMessageActionStrategy.class);

	/**
	 * If the parameter action is of type {@link DisplayMessageRAO} PromotionResultModel for potential promotion message is
	 * created.
	 *
	 * @return list of {@link PromotionResultModel} as a result of the {@link DisplayMessageRAO} application.
	 */
	@Override
	public List<PromotionResultModel> apply(final AbstractRuleActionRAO action)
	{
		if (!(action instanceof DisplayMessageRAO))
		{
			LOG.error("cannot apply {}, action is not of type DisplayMessageRAO", this.getClass().getSimpleName());
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

		final RuleBasedPotentialPromotionMessageActionModel actionModel = createPromotionAction(promoResult, action);
		handleActionMetadata(action, actionModel);

		supplementMessageActionModelWithParameters((DisplayMessageRAO) action, actionModel);

		getModelService().saveAll(promoResult, actionModel);

		return Collections.singletonList(promoResult);
	}

	protected void supplementMessageActionModelWithParameters(final DisplayMessageRAO action,
			final RuleBasedPotentialPromotionMessageActionModel actionModel)
	{
		if (MapUtils.isNotEmpty(action.getParameters()))
		{
			actionModel.setParameters(
					action.getParameters().entrySet().stream().map(this::convertToActionParameterModel).collect(Collectors.toList()));
		}
	}

	protected PromotionActionParameterModel convertToActionParameterModel(final Entry<String, Object> actionParameterEntry)
	{
		final PromotionActionParameterModel actionParameterModel = getModelService().create(PromotionActionParameterModel.class);
		actionParameterModel.setUuid(actionParameterEntry.getKey());
		actionParameterModel.setValue(actionParameterEntry.getValue());
		return actionParameterModel;
	}

	@Override
	public void undo(final ItemModel item)
	{
		if (item instanceof RuleBasedPotentialPromotionMessageActionModel)
		{
			final RuleBasedPotentialPromotionMessageActionModel action = (RuleBasedPotentialPromotionMessageActionModel) item;
			handleUndoActionMetadata(action);
			removeMessageActionModelParameters(action);
			undoInternal(action);
		}
	}

	protected void removeMessageActionModelParameters(final RuleBasedPotentialPromotionMessageActionModel action)
	{
		if (CollectionUtils.isNotEmpty(action.getParameters()))
		{
			action.getParameters().stream().forEach(param -> getModelService().remove(param));
		}
	}
}
