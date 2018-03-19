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
package com.sap.rediscart.commerceservices.order.dao.impl;

import de.hybris.platform.basecommerce.model.site.BaseSiteModel;
import de.hybris.platform.commerceservices.order.dao.CommerceCartDao;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.core.model.user.UserModel;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.annotation.Resource;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;

import com.sap.rediscart.util.RedisKeyGenerator;


/**
 * @author Henter Liu (henterji@163.com)
 */
public class CustomizedCommerceCartDao extends AbstractCustomizedCartDao implements CommerceCartDao
{
	@SuppressWarnings("unused")
	private static final Logger LOG = Logger.getLogger(CustomizedCommerceCartDao.class);

	private final Comparator<CartModel> c = (o1, o2) -> o2.getModifiedtime().compareTo(o1.getModifiedtime());

	private RedisTemplate<String, Object> redisTemplate;
	private ValueOperations<String, Object> valueOps;
	private SetOperations<String, Object> setOps;

	@Resource
	public void setRedisTemplate(final RedisTemplate<String, Object> redisTemplate)
	{
		this.redisTemplate = redisTemplate;
		this.valueOps = this.redisTemplate.opsForValue();
		this.setOps = this.redisTemplate.opsForSet();
	}

	@Resource
	private RedisKeyGenerator redisKeyGenerator;

	protected CartModel getCartByCode(final String cartCode)
	{
		if (StringUtils.isBlank(cartCode))
		{
			return null;
		}
		final String cartKey = redisKeyGenerator.generateCartKey(cartCode);
		final Object value = valueOps.get(cartKey);
		if (value == null)
		{
			return null;
		}
		return getModelService().get(value);
	}

	@Override
	public CartModel getCartForGuidAndSiteAndUser(final String guid, final BaseSiteModel site, final UserModel user)
	{
		if (guid != null)
		{
			final String guidKey = redisKeyGenerator.generateGuidKey(guid);
			final String userIdKey = redisKeyGenerator.generateUserIdKey(user.getUid());
			final String siteIdKey = redisKeyGenerator.generateSiteIdKey(site.getUid());
			final Set<Object> set = setOps.intersect(null, Arrays.asList(guidKey, userIdKey, siteIdKey));
			final List<CartModel> cartModels = new ArrayList<>();
			for (final Object key : set)
			{
				final String cartCode = key.toString();
				cartModels.add(getCartByCode(cartCode));
			}

			Collections.sort(cartModels, c);

			if (!cartModels.isEmpty())
			{
				return cartModels.get(0);
			}
			return null;
		}
		else
		{
			return getCartForSiteAndUser(site, user);
		}
	}

	@Override
	public CartModel getCartForGuidAndSite(final String guid, final BaseSiteModel site)
	{
		final String guidKey = redisKeyGenerator.generateGuidKey(guid);
		final String siteIdKey = redisKeyGenerator.generateSiteIdKey(site.getUid());
		final Set<Object> set = setOps.intersect(null, Arrays.asList(guidKey, siteIdKey));
		final List<CartModel> cartModels = new ArrayList<>();
		for (final Object key : set)
		{
			final String cartCode = key.toString();
			cartModels.add(getCartByCode(cartCode));
		}

		Collections.sort(cartModels, c);

		if (!cartModels.isEmpty())
		{
			return cartModels.get(0);
		}
		return null;
	}

	@Override
	public CartModel getCartForCodeAndUser(final String code, final UserModel user)
	{
		final String codeKey = redisKeyGenerator.generateCodeKey(code);
		final String userIdKey = redisKeyGenerator.generateUserIdKey(user.getUid());
		final Set<Object> set = setOps.intersect(null, Arrays.asList(codeKey, userIdKey));
		final List<CartModel> cartModels = new ArrayList<>();
		for (final Object key : set)
		{
			final String cartCode = key.toString();
			cartModels.add(getCartByCode(cartCode));
		}

		Collections.sort(cartModels, c);

		if (!cartModels.isEmpty())
		{
			return cartModels.get(0);
		}
		return null;
	}

	@Override
	public CartModel getCartForSiteAndUser(final BaseSiteModel site, final UserModel user)
	{
		final String userIdKey = redisKeyGenerator.generateUserIdKey(user.getUid());
		final String siteIdKey = redisKeyGenerator.generateSiteIdKey(site.getUid());
		final Set<Object> set = setOps.intersect(null, Arrays.asList(userIdKey, siteIdKey));
		final List<CartModel> cartModels = new ArrayList<>();
		for (final Object key : set)
		{
			final String cartCode = key.toString();
			final CartModel cartModel = getCartByCode(cartCode);
			if (cartModel.getSaveTime() == null)
			{
				cartModels.add(cartModel);
			}
		}

		Collections.sort(cartModels, c);

		if (!cartModels.isEmpty())
		{
			return cartModels.get(0);
		}
		return null;
	}

	@Override
	public List<CartModel> getCartsForSiteAndUser(final BaseSiteModel site, final UserModel user)
	{
		final String userIdKey = redisKeyGenerator.generateUserIdKey(user.getUid());
		final String siteIdKey = redisKeyGenerator.generateSiteIdKey(site.getUid());
		final Set<Object> set = setOps.intersect(null, Arrays.asList(userIdKey, siteIdKey));
		final List<CartModel> cartModels = new ArrayList<>();
		for (final Object key : set)
		{
			final String cartCode = key.toString();
			final CartModel cartModel = getCartByCode(cartCode);
			if (cartModel.getSaveTime() == null)
			{
				cartModels.add(cartModel);
			}
		}

		Collections.sort(cartModels, c);

		return cartModels;
	}

	@Override
	public List<CartModel> getCartsForRemovalForSiteAndUser(final Date modifiedBefore, final BaseSiteModel site,
			final UserModel user)
	{
		final List<CartModel> cartModels = new ArrayList<>();
		final String siteIdKey = redisKeyGenerator.generateSiteIdKey(site.getUid());
		Set<Object> set = null;
		if (user == null)
		{
			set = setOps.intersect(null, Arrays.asList(siteIdKey));
		}
		else
		{
			final String userIdKey = redisKeyGenerator.generateUserIdKey(user.getUid());
			set = setOps.intersect(null, Arrays.asList(userIdKey, siteIdKey));
		}
		for (final Object key : set)
		{
			final String cartCode = key.toString();
			final CartModel cartModel = getCartByCode(cartCode);
			if (cartModel != null)
			{
				final Date modifiedtime = cartModel.getModifiedtime();
				if ((modifiedtime.before(modifiedBefore) || modifiedtime.equals(modifiedBefore)) && cartModel.getSaveTime() == null)
				{
					cartModels.add(cartModel);
				}
			}
		}

		if (CollectionUtils.isNotEmpty(cartModels))
		{
			Collections.sort(cartModels, c);
		}

		return cartModels;
	}

	// add following properties for ootb reference
	private FlexibleSearchService flexibleSearchService;
	private ModelService modelService;

	@Required
	public void setFlexibleSearchService(final FlexibleSearchService flexibleSearch)
	{
		this.flexibleSearchService = flexibleSearch;
	}

	public FlexibleSearchService getFlexibleSearchService()
	{
		return flexibleSearchService;
	}

	@Required
	public void setModelService(final ModelService modelService)
	{
		this.modelService = modelService;
	}

	public ModelService getModelService()
	{
		return modelService;
	}
}
