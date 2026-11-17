const PAGE_SIZE = 50;

const businessTypeList = async (z, bundle) => {
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
            name: 'businessTypeList',
            search: bundle.meta.searchTerm || '',
            limit: PAGE_SIZE
        }
    });
  
    const list = response.data?.data?.list || [];

  return list.map((businessType) => ({
    id: businessType.id.toString(),
    name: businessType.name
  }));
  };
  
  module.exports = {
    key: 'businessType_list',
    noun: 'Business Type List',
    list: {
        display: {
          label: 'List Business type',
          description: 'Fetches a list of business type for dropdown.',
          hidden: true
        },
        operation: {
          perform: businessTypeList,
          canPaginate: true
        },
      },
  };
  