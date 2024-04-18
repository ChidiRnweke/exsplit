import type { components as ExpenseListComponents } from '../schemas/exsplit.spec.ExpenseListService';

export type CreateExpenseListInput =
	ExpenseListComponents['schemas']['CreateExpenseListRequestContent'];
export type CreateExpenseListOutput =
	ExpenseListComponents['schemas']['CreateExpenseListResponseContent'];
export type UpdateExpenseListInput =
	ExpenseListComponents['schemas']['UpdateExpenseListRequestContent'];
export type GetExpenseListsOutput =
	ExpenseListComponents['schemas']['GetExpenseListsResponseContent'];
export type GetExpenseListOutput =
	ExpenseListComponents['schemas']['GetExpenseListResponseContent'];
export type GetSettledExpenseListsOutput =
	ExpenseListComponents['schemas']['GetSettledExpenseListsResponseContent'];
export type SettleExpenseListInput =
	ExpenseListComponents['schemas']['SettleExpenseListRequestContent'];

export type ExpenseListId = { expenseListId: string };
