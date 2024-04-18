import type { components as ExpenseComponents } from '../schemas/exsplit.spec.ExpenseService';

export type CreateExpenseInput = ExpenseComponents['schemas']['CreateExpenseRequestContent'];
export type CreateExpenseOutput = ExpenseComponents['schemas']['CreateExpenseResponseContent'];
export type GetExpenseOutput = ExpenseComponents['schemas']['GetExpenseResponseContent'];
export type UpdateExpenseInput = ExpenseComponents['schemas']['UpdateExpenseRequestContent'];
export type ExpenseId = { expenseId: string };
