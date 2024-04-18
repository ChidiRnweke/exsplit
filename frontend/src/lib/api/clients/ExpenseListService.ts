import createClient from 'openapi-fetch';
import { throwIfError } from './util/ErrorHandling';
import type { CircleId } from '../types/circles';
import type {
	CreateExpenseListInput,
	CreateExpenseListOutput,
	GetExpenseListsOutput,
	ExpenseListId,
	GetExpenseListOutput,
	UpdateExpenseListInput,
	SettleExpenseListInput,
	GetSettledExpenseListsOutput
} from '../types/expenseList';
import type { paths as ExpenseListPaths } from '../schemas/exsplit.spec.ExpenseListService';
import { authMiddleware } from './util/Auth';

export interface ExpenseListService {
	createExpenseList: (
		circleId: CircleId,
		expenseListInput: CreateExpenseListInput
	) => Promise<CreateExpenseListOutput>;
	getExpenseLists: (circleId: CircleId) => Promise<GetExpenseListsOutput>;
	getExpenseList: (expenseListId: ExpenseListId) => Promise<GetExpenseListOutput>;
	deleteExpenseList: (expenseListId: ExpenseListId) => Promise<void>;
	updateExpenseList: (
		expenseListId: ExpenseListId,
		updateExpenseListInput: UpdateExpenseListInput
	) => Promise<void>;
	settleExpenseList: (
		expenseListId: ExpenseListId,
		settleExpenseListInput: SettleExpenseListInput
	) => Promise<void>;
	getSettledExpenseList: (expenseListId: ExpenseListId) => Promise<GetSettledExpenseListsOutput>;
}

class ExpenseListClient implements ExpenseListService {
	private client = createClient<ExpenseListPaths>({ baseUrl: '/' });

	constructor() {
		this.client.use(authMiddleware);
	}

	createExpenseList = async (
		circleId: CircleId,
		expenseListInput: CreateExpenseListInput
	): Promise<CreateExpenseListOutput> => {
		const { error, data } = await this.client.POST('/api/circles/{circleId}/expenseLists', {
			params: { path: circleId },
			body: expenseListInput
		});
		return throwIfError(data, error);
	};

	getExpenseLists = async (circleId: CircleId): Promise<GetExpenseListsOutput> => {
		const { error, data } = await this.client.GET('/api/circles/{circleId}/expenseLists', {
			params: { path: circleId }
		});
		return throwIfError(data, error);
	};

	getExpenseList = async (expenseListId: ExpenseListId): Promise<GetExpenseListOutput> => {
		const { error, data } = await this.client.GET('/api/expenseLists/{expenseListId}', {
			params: { path: expenseListId }
		});
		return throwIfError(data, error);
	};

	deleteExpenseList = async (expenseListId: ExpenseListId): Promise<void> => {
		const { error } = await this.client.DELETE('/api/expenseLists/{expenseListId}', {
			params: { path: expenseListId }
		});
		throwIfError(undefined, error);
	};

	updateExpenseList = async (
		expenseListId: ExpenseListId,
		updateExpenseListInput: UpdateExpenseListInput
	): Promise<void> => {
		const { error } = await this.client.PUT('/api/expenseLists/{expenseListId}', {
			params: { path: expenseListId },
			body: updateExpenseListInput
		});
		throwIfError(undefined, error);
	};

	settleExpenseList = async (
		expenseListId: ExpenseListId,
		settleExpenseListInput: SettleExpenseListInput
	): Promise<void> => {
		const { error, data } = await this.client.POST('/api/expenseLists/{expenseListId}/settle', {
			params: { path: expenseListId },
			body: settleExpenseListInput
		});
		return throwIfError(data, error);
	};

	getSettledExpenseList = async (
		expenseListId: ExpenseListId
	): Promise<GetSettledExpenseListsOutput> => {
		const { error, data } = await this.client.GET('/api/expenseLists/{expenseListId}/settle', {
			params: { path: expenseListId }
		});
		return throwIfError(data, error);
	};
}

export const expenseListClient = new ExpenseListClient();
