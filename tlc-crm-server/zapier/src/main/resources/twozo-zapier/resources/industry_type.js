const PAGE_SIZE = 50;

const industryType = async (z, bundle) => {
    const response = await z.request({
        method: 'POST',
        url: 'https://crm.twozo.io/api/zap/dropdown',
        headers: {
          'Authorization': `Bearer ${bundle.authData.access_token}`,
          'X-EMAIL': bundle.authData.email,
          'Content-Type': 'application/json'
        },
        body: {
            page: bundle.meta.page,
            name: 'industryTypeList',
            search: bundle.meta.searchTerm || '',
            limit: PAGE_SIZE
        }
    });
  
    const list = response.data?.data?.list || [];

  return list.map((indus) => ({
    id: indus.id.toString(),
    name: indus.name
  }));
  };
  
  module.exports = {
    key: 'industryType_list',
    noun: 'Industry Type List',
    list: {
        display: {
          label: 'List Industry type',
          description: 'Fetches a list of industries for dropdown.',
          hidden: true
        },
        operation: {
          perform: industryType,
          canPaginate: true
        },
      },
  };
  