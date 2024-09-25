package ru.aritmos.service;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import jakarta.inject.Singleton;
import ru.aritmos.model.Branch;

@Singleton
public class BranchFetchData implements DataFetcher<Iterable<Branch>> {
  final BranchService branchService;

  public BranchFetchData(BranchService branchService) {
    this.branchService = branchService;
  }

  @Override
  public Iterable<Branch> get(DataFetchingEnvironment env) {

    return branchService.getBranches().values();
  }
}
