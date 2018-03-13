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
package com.sap.rediscart.strategy.impl;

import de.hybris.platform.core.model.ItemModel;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.promotions.jalo.PromotionResult;
import de.hybris.platform.promotions.model.CachedPromotionResultModel;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author Henter Liu (henterji@163.com)
 */
public class PromotionResultSaveStrategy extends AbstractModelSaveStrategy
{
	private static final Logger LOG = LoggerFactory.getLogger(PromotionResultSaveStrategy.class);

	@Override
	protected boolean doBeforeSave(final Collection<? extends Object> toSave, final ItemModel model)
	{
		return false;
	}

	@Override
	protected boolean doAfterSave(final ItemModel model)
	{
		final CachedPromotionResultModel prModel = (CachedPromotionResultModel) model;
		final PromotionResult pr = getModelService().getSource(prModel);
		final CartModel cart = (CartModel) prModel.getOrder();
		final String prKey = getRedisKeyGenerator().generatePromotionResultKey(cart, prModel);
		getRedisTemplate().opsForValue().set(prKey, pr);
		LOG.debug("CachedPromotionResult saved to redis: " + prKey);
		return true;
	}

	@Override
	protected boolean doBeforeRemove(final Collection<? extends Object> toSave, final ItemModel model)
	{
		final CachedPromotionResultModel prModel = (CachedPromotionResultModel) model;
		final String cartCode = prModel.getOrder().getCode();
		final String prKey = getRedisKeyGenerator().generatePromotionResultKey(cartCode, prModel.getPk().toString());
		getRedisTemplate().delete(prKey);
		LOG.debug("CachedPromotionResult removed from redis: " + prKey);
		return true;
	}

	@Override
	protected boolean doAfterRemove(final ItemModel model)
	{
		return false;
	}
}
