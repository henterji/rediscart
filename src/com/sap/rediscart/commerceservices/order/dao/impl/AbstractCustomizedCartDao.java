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

import de.hybris.platform.commerceservices.search.pagedata.PageableData;
import de.hybris.platform.commerceservices.search.pagedata.PaginationData;
import de.hybris.platform.core.model.order.CartModel;

import java.util.List;


/**
 * @author Henter Liu (henterji@163.com)
 */
public abstract class AbstractCustomizedCartDao
{
	protected List<CartModel> getSubList(final List<CartModel> carts, final int currentPage, final int pageSize)
	{
		final int listSize = (null == carts) ? 0 : carts.size();
		final int startIndex = currentPage * pageSize;
		if (startIndex + pageSize < listSize)
		{
			return carts.subList(startIndex, startIndex + pageSize);
		}
		else
		{
			return carts.subList(startIndex, listSize);
		}
	}

	protected <T> PaginationData createPagination(final PageableData pageableData, final long total)
	{
		final PaginationData paginationData = new PaginationData();
		paginationData.setPageSize(pageableData.getPageSize());
		paginationData.setSort(pageableData.getSort());
		paginationData.setTotalNumberOfResults(total);

		// Calculate the number of pages
		paginationData.setNumberOfPages(
				(int) Math.ceil(((double) paginationData.getTotalNumberOfResults()) / paginationData.getPageSize()));

		// Work out the current page, fixing any invalid page values
		paginationData.setCurrentPage(Math.max(0, Math.min(paginationData.getNumberOfPages(), pageableData.getCurrentPage())));

		return paginationData;
	}
}
