const PAGE_SIZE = 50;

const pipelineStageList = async (z, bundle) => {
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
            name: 'stageList',
            search: bundle.meta.searchTerm || '',
            limit: PAGE_SIZE
        }
    });
  
    const list = response.data?.data?.list || [];

  return list.map((result) => ({
    id: result.id.toString(),
    name: result.name
  }));
  };
  
  module.exports = {
    key: 'pipelineStage_list',
    noun: 'Pipeline Stage List',
    list: {
        display: {
          label: 'List Pipeline Stages',
          description: 'Fetches a list of pipeline stages for dropdown.',
          hidden: true
        },
        operation: {
          perform: pipelineStageList,
          canPaginate: true
        },
      },
  };
  