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
import de.hybris.platform.commerceservices.order.dao.SaveCartDao;
import de.hybris.platform.commerceservices.search.pagedata.PageableData;
import de.hybris.platform.commerceservices.search.pagedata.SearchPageData;
import de.hybris.platform.core.enums.OrderStatus;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.core.model.user.UserModel;
import de.hybris.platform.servicelayer.model.ModelService;

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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;

import com.sap.rediscart.util.RedisKeyGenerator;


/**
 * @author Henter Liu (henterji@163.com)
 */
public class CustomizedSaveCartDao extends CustomizedCommerceCartDao implements SaveCartDao
{
	@SuppressWarnings("unused")
	private static final Logger LOG = Logger.getLogger(CustomizedSaveCartDao.class);

	protected static final String SORT_CODE_BY_DATE_MODIFIED = "byDateModified";
	protected static final String SORT_CODE_BY_DATE_SAVED = "byDateSaved";
	protected static final String SORT_CODE_BY_NAME = "byName";
	protected static final String SORT_CODE_BY_CODE = "byCode";
	protected static final String SORT_CODE_BY_TOTAL = "byTotal";

	private SetOperations<String, Object> setOps;
	private RedisTemplate<String, Object> redisTemplate;

	@Override
	@Resource
	public void setRedisTemplate(final RedisTemplate<String, Object> redisTemplate)
	{
		this.redisTemplate = redisTemplate;
		this.setOps = this.redisTemplate.opsForSet();
	}

	@Resource
	private RedisKeyGenerator redisKeyGenerator;

	@Resource
	private ModelService modelService;

	@Override
	public List<CartModel> getSavedCartsForRemovalForSite(final BaseSiteModel site)
	{
		final String siteIdKey = redisKeyGenerator.generateSiteIdKey(site.getUid());
		final Set<Object> set = setOps.intersect(null, Arrays.asList(siteIdKey));
		final List<CartModel> cartModels = new ArrayList<CartModel>();
		for (final Object key : set)
		{
			final String cartCode = key.toString();
			final CartModel cartModel = getCartByCode(cartCode);
			if (cartModel.getSaveTime() != null)
			{
				final Date expirationTime = cartModel.getExpirationTime();
				final Date now = new Date();
				if (expirationTime.before(now) || expirationTime.equals(now))
				{
					cartModels.add(cartModel);
				}
			}
		}

		Collections.sort(cartModels, new Comparator<CartModel>()
		{
			@Override
			public int compare(final CartModel o1, final CartModel o2)
			{
				return o2.getModifiedtime().compareTo(o1.getModifiedtime());
			}
		});

		return cartModels;
	}

	@Override
	public SearchPageData<CartModel> getSavedCartsForSiteAndUser(final PageableData pageableData, final BaseSiteModel baseSite,
			final UserModel user, final List<OrderStatus> orderStatus)
	{
		final List<CartModel> cartModels = new ArrayList<CartModel>();
		if (baseSite != null)
		{
			if (CollectionUtils.isNotEmpty(orderStatus))
			{
				final String userIdKey = redisKeyGenerator.generateUserIdKey(user.getUid());
				final String siteIdKey = redisKeyGenerator.generateSiteIdKey(baseSite.getUid());
				final Set<Object> set = setOps.intersect(null, Arrays.asList(userIdKey, siteIdKey));
				for (final Object key : set)
				{
					final String cartCode = key.toString();
					final CartModel cartModel = getCartByCode(cartCode);
					if (cartModel.getSaveTime() != null)
					{
						final OrderStatus cartStatus = cartModel.getStatus();
						if (orderStatus.contains(cartStatus))
						{
							cartModels.add(cartModel);
						}
					}
				}
			}
			else
			{
				final String userIdKey = redisKeyGenerator.generateUserIdKey(user.getUid());
				final String siteIdKey = redisKeyGenerator.generateSiteIdKey(baseSite.getUid());
				final Set<Object> set = setOps.intersect(null, Arrays.asList(userIdKey, siteIdKey));
				for (final Object key : set)
				{
					final String cartCode = key.toString();
					final CartModel cartModel = getCartByCode(cartCode);
					if (cartModel.getSaveTime() != null)
					{
						cartModels.add(cartModel);
					}
				}
			}
		}
		else
		{
			if (CollectionUtils.isNotEmpty(orderStatus))
			{
				final String userIdKey = redisKeyGenerator.generateUserIdKey(user.getUid());
				final Set<Object> set = setOps.intersect(null, Arrays.asList(userIdKey));
				for (final Object key : set)
				{
					final String cartCode = key.toString();
					final CartModel cartModel = getCartByCode(cartCode);
					if (cartModel.getSaveTime() != null)
					{
						final OrderStatus cartStatus = cartModel.getStatus();
						if (orderStatus.contains(cartStatus))
						{
							cartModels.add(cartModel);
						}
					}
				}
			}
			else
			{
				final String userIdKey = redisKeyGenerator.generateUserIdKey(user.getUid());
				final Set<Object> set = setOps.intersect(null, Arrays.asList(userIdKey));
				for (final Object key : set)
				{
					final String cartCode = key.toString();
					final CartModel cartModel = getCartByCode(cartCode);
					if (cartModel.getSaveTime() != null)
					{
						cartModels.add(cartModel);
					}
				}
			}
		}

		String sortCode = pageableData.getSort();
		if (StringUtils.isBlank(sortCode))
		{
			sortCode = SORT_CODE_BY_DATE_MODIFIED;
		}
		switch (sortCode)
		{
			case SORT_CODE_BY_DATE_MODIFIED:
				sort(cartModels, Arrays.asList(new Comparator<CartModel>()
				{
					@Override
					public int compare(final CartModel o1, final CartModel o2)
					{
						return o2.getModifiedtime().compareTo(o1.getModifiedtime());
					}
				}));
				break;
			case SORT_CODE_BY_DATE_SAVED:
				sort(cartModels, Arrays.asList(new Comparator<CartModel>()
				{
					@Override
					public int compare(final CartModel o1, final CartModel o2)
					{
						return o2.getSaveTime().compareTo(o1.getSaveTime());
					}
				}));
				break;
			case SORT_CODE_BY_NAME:
				sort(cartModels, Arrays.asList(new Comparator<CartModel>()
				{
					@Override
					public int compare(final CartModel o1, final CartModel o2)
					{
						return o1.getName().compareTo(o2.getName());
					}
				}, new Comparator<CartModel>()
				{
					@Override
					public int compare(final CartModel o1, final CartModel o2)
					{
						return o2.getModifiedtime().compareTo(o1.getModifiedtime());
					}
				}));
				break;
			case SORT_CODE_BY_CODE:
				sort(cartModels, Arrays.asList(new Comparator<CartModel>()
				{
					@Override
					public int compare(final CartModel o1, final CartModel o2)
					{
						return o1.getCode().compareTo(o2.getCode());
					}
				}, new Comparator<CartModel>()
				{
					@Override
					public int compare(final CartModel o1, final CartModel o2)
					{
						return o2.getModifiedtime().compareTo(o1.getModifiedtime());
					}
				}));
				break;
			case SORT_CODE_BY_TOTAL:
				sort(cartModels, Arrays.asList(new Comparator<CartModel>()
				{
					@Override
					public int compare(final CartModel o1, final CartModel o2)
					{
						return o1.getTotalPrice().compareTo(o2.getTotalPrice());
					}
				}, new Comparator<CartModel>()
				{
					@Override
					public int compare(final CartModel o1, final CartModel o2)
					{
						return o2.getModifiedtime().compareTo(o1.getModifiedtime());
					}
				}));
				break;
		}

		final List<CartModel> subCartModels = getSubList(cartModels, pageableData.getCurrentPage(), pageableData.getPageSize());

		final SearchPageData<CartModel> result = new SearchPageData<CartModel>();
		result.setResults(subCartModels);
		result.setPagination(createPagination(pageableData, cartModels.size()));
		return result;
	}

	private void sort(final List<CartModel> list, final List<Comparator<CartModel>> comList)
	{
		if (comList == null)
		{
			return;
		}
		final Comparator<CartModel> cmp = new Comparator<CartModel>()
		{
			@Override
			public int compare(final CartModel o1, final CartModel o2)
			{
				for (final Comparator<CartModel> comparator : comList)
				{
					if (comparator.compare(o1, o2) > 0)
					{
						return 1;
					}
					else if (comparator.compare(o1, o2) < 0)
					{
						return -1;
					}
				}
				return 0;
			}
		};
		Collections.sort(list, cmp);
	}

	@Override
	public Integer getSavedCartsCountForSiteAndUser(final BaseSiteModel baseSite, final UserModel user)
	{
		final List<CartModel> cartModels = new ArrayList<CartModel>();

		if (baseSite != null)
		{
			final String userIdKey = redisKeyGenerator.generateUserIdKey(user.getUid());
			final String siteIdKey = redisKeyGenerator.generateSiteIdKey(baseSite.getUid());
			final Set<Object> set = setOps.intersect(null, Arrays.asList(userIdKey, siteIdKey));
			for (final Object key : set)
			{
				final String cartCode = key.toString();
				final CartModel cartModel = getCartByCode(cartCode);
				if (cartModel.getSaveTime() != null)
				{
					cartModels.add(cartModel);
				}
			}
		}
		else
		{
			final String userIdKey = redisKeyGenerator.generateUserIdKey(user.getUid());
			final Set<Object> set = setOps.intersect(null, Arrays.asList(userIdKey));
			for (final Object key : set)
			{
				final String cartCode = key.toString();
				final CartModel cartModel = getCartByCode(cartCode);
				if (cartModel.getSaveTime() != null)
				{
					cartModels.add(cartModel);
				}
			}
		}

		return Integer.valueOf(cartModels.size());
	}
}
