import createClient from 'openapi-fetch';
import { throwIfError } from './util/ErrorHandling';
import type {
	CreateExpenseInput,
	CreateExpenseOutput,
	ExpenseId,
	GetExpenseOutput,
	UpdateExpenseInput
} from '../types/expense';
import type { ExpenseListId } from '../types/expenseList';
import type { paths as ExpensePaths } from '../schemas/exsplit.spec.ExpenseService';
import { authMiddleware } from './util/Auth';

export interface ExpenseService {
	createExpense: (
		expenseListId: ExpenseListId,
		expenseInput: CreateExpenseInput
	) => Promise<CreateExpenseOutput>;
	getExpense: (expenseId: ExpenseId) => Promise<GetExpenseOutput>;
	updateExpense: (expenseId: ExpenseId, updateExpenseInput: UpdateExpenseInput) => Promise<void>;
	deleteExpense: (expenseId: ExpenseId) => Promise<void>;
}
class ExpenseClient implements ExpenseService {
	private client = createClient<ExpensePaths>({ baseUrl: '/' });
	constructor() {
		this.client.use(authMiddleware);
	}

	createExpense = async (
		expenseListId: ExpenseListId,
		expenseInput: CreateExpenseInput
	): Promise<CreateExpenseOutput> => {
		const { error, data } = await this.client.POST('/api/expense_lists/{expenseListId}/expenses', {
			params: { path: expenseListId },
			body: expenseInput
		});
		return throwIfError(data, error);
	};

	getExpense = async (expenseId: ExpenseId): Promise<GetExpenseOutput> => {
		const { error, data } = await this.client.GET('/api/expenses/{expenseId}', {
			params: { path: expenseId }
		});
		return throwIfError(data, error);
	};

	updateExpense = async (
		expenseId: ExpenseId,
		updateExpenseInput: UpdateExpenseInput
	): Promise<void> => {
		const { error } = await this.client.PATCH('/api/expenses/{expenseId}', {
			params: { path: expenseId },
			body: updateExpenseInput
		});
		throwIfError(undefined, error);
	};

	deleteExpense = async (expenseId: ExpenseId): Promise<void> => {
		const { error } = await this.client.DELETE('/api/expenses/{expenseId}', {
			params: { path: expenseId }
		});
		throwIfError(undefined, error);
	};
}

export const expenseClient = new ExpenseClient();
