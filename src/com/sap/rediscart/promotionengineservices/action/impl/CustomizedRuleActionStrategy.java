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

import de.hybris.platform.promotionengineservices.action.impl.AbstractRuleActionStrategy;
import de.hybris.platform.promotionengineservices.model.AbstractRuleBasedPromotionActionModel;
import de.hybris.platform.promotions.model.AbstractPromotionActionModel;
import de.hybris.platform.promotions.model.PromotionResultModel;
import de.hybris.platform.ruleengineservices.rao.AbstractRuleActionRAO;

import java.util.Collection;
import java.util.HashSet;


/**
 * @author Henter Liu (henterji@163.com)
 */
public abstract class CustomizedRuleActionStrategy<RULE_BASED_ACTION extends AbstractRuleBasedPromotionActionModel>
		extends AbstractRuleActionStrategy<RULE_BASED_ACTION>
{
	@Override
	protected RULE_BASED_ACTION createPromotionAction(final PromotionResultModel promotionResult,
			final AbstractRuleActionRAO action)
	{
		final RULE_BASED_ACTION result = super.createPromotionAction(promotionResult, action);
		Collection<AbstractPromotionActionModel> actions = promotionResult.getActions();
		if (actions == null)
		{
			actions = new HashSet<AbstractPromotionActionModel>();
		}
		actions.add(result);
		promotionResult.setActions(actions);

		return result;
	}
}
