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

import java.util.Collection;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.rediscart.jalo.order.RedisCart;
import com.sap.rediscart.model.order.RedisCartModel;


/**
 * @author Henter Liu (henterji@163.com)
 */
public class CartModelSaveStrategy extends AbstractModelSaveStrategy
{
	private static final Logger LOG = LoggerFactory.getLogger(CartModelSaveStrategy.class);

	@Override
	protected boolean doAfterSave(final ItemModel model)
	{
		final CartModel cart = (CartModel) model;
		final RedisCart redisCart = getModelService().getSource(cart);
		final String cartKey = getRedisKeyGenerator().generateCartKey(cart);
		getValueOps().set(cartKey, redisCart);
		LOG.debug("Cart saved to redis: " + cartKey);

		if (cart instanceof RedisCartModel)
		{
			final RedisCartModel redisCartModel = (RedisCartModel) cart;
			final String code = redisCartModel.getCode();
			if (StringUtils.isNotBlank(code))
			{
				getSetOps().remove(getRedisKeyGenerator().generateCodeKey(code), code);
				getSetOps().add(getRedisKeyGenerator().generateCodeKey(code), code);
			}
			final String userId = redisCartModel.getUser().getUid();
			if (StringUtils.isNotBlank(userId))
			{
				getSetOps().remove(getRedisKeyGenerator().generateUserIdKey(userId), code);
				getSetOps().add(getRedisKeyGenerator().generateUserIdKey(userId), code);
			}
			final String guid = redisCartModel.getGuid();
			if (StringUtils.isNotBlank(guid))
			{
				getSetOps().remove(getRedisKeyGenerator().generateGuidKey(guid), code);
				getSetOps().add(getRedisKeyGenerator().generateGuidKey(guid), code);
			}
			final String siteId = redisCartModel.getSite().getUid();
			if (StringUtils.isNotBlank(siteId))
			{
				getSetOps().remove(getRedisKeyGenerator().generateSiteIdKey(siteId), code);
				getSetOps().add(getRedisKeyGenerator().generateSiteIdKey(siteId), code);
			}
		}

		return true;
	}

	@Override
	protected boolean doBeforeSave(final Collection<? extends Object> toSave, final ItemModel model)
	{
		// TODO: before saving
		return true;
	}

	@Override
	protected boolean doBeforeRemove(final Collection<? extends Object> toSave, final ItemModel model)
	{
		final CartModel cart = (CartModel) model;

		final String cartKey = getRedisKeyGenerator().generateCartKey(cart);
		getRedisTemplate().delete(cartKey);
		LOG.debug("Cart deleted from redis: " + cartKey);

		if (cart instanceof RedisCartModel)
		{
			final RedisCartModel redisCartModel = (RedisCartModel) cart;
			final String code = redisCartModel.getCode();
			if (StringUtils.isNotBlank(code))
			{
				getSetOps().remove(getRedisKeyGenerator().generateCodeKey(code), code);
			}
			final String userId = redisCartModel.getUser().getUid();
			if (StringUtils.isNotBlank(userId))
			{
				getSetOps().remove(getRedisKeyGenerator().generateUserIdKey(userId), code);
			}
			final String guid = redisCartModel.getGuid();
			if (StringUtils.isNotBlank(guid))
			{
				getSetOps().remove(getRedisKeyGenerator().generateGuidKey(guid), code);
			}
			final String siteId = redisCartModel.getSite().getUid();
			if (StringUtils.isNotBlank(siteId))
			{
				getSetOps().remove(getRedisKeyGenerator().generateSiteIdKey(siteId), code);
			}

			final String prPattern = getRedisKeyGenerator().generateCartKey(code);
			final Set prKeys = getRedisTemplate().keys(prPattern + ":promotionResult:*");
			if (CollectionUtils.isNotEmpty(prKeys))
			{
				for (final Object prKey : prKeys)
				{
					getRedisTemplate().delete(prKey.toString());
				}
			}

			final String vrPattern = getRedisKeyGenerator().generateCartKey(code);
			final Set vrKeys = getRedisTemplate().keys(vrPattern + ":voucherResult:*");
			if (CollectionUtils.isNotEmpty(vrKeys))
			{
				for (final Object vrKey : vrKeys)
				{
					getRedisTemplate().delete(vrKey.toString());
				}
			}
		}

		return true;
	}

	@Override
	protected boolean doAfterRemove(final ItemModel model)
	{
		return false;
	}
}
