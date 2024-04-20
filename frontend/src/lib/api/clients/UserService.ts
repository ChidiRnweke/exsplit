import createClient from 'openapi-fetch';
import { type APIResponse } from './util/ErrorHandling';
import type { paths } from '../schemas/exsplit.spec.UserService';
import type {
	LoginInput,
	LoginOutput,
	RefreshInput,
	RefreshOutput,
	RegisterInput,
	RegisterOutput,
	TokenKind
} from '../types/user';

export interface UserService {
	login: (credentials: LoginInput) => Promise<APIResponse<LoginOutput>>;
	refresh: (refreshToken: RefreshInput) => Promise<APIResponse<RefreshOutput>>;
	register: (credentials: RegisterInput) => Promise<APIResponse<RegisterOutput>>;
}

class UserClient implements UserService {
	private client = createClient<paths>({ baseUrl: '/' });

	login = async (credentials: LoginInput): Promise<APIResponse<LoginOutput>> => {
		return await this.client
			.POST('/api/auth/login', { body: credentials })
			.catch((e) => {
				console.error(e);
				throw e;
			})
			.then((res) => {
				const { data } = res;
				if (data) {
					this.addTokenToStorage('accessToken', data.accessToken);
					this.addTokenToStorage('refreshToken', data.refreshToken);
				}
				return res;
			});
	};

	refresh = async (refreshToken: RefreshInput): Promise<APIResponse<RefreshOutput>> => {
		return await this.client
			.POST('/api/auth/refresh', { body: refreshToken })
			.catch((e) => {
				console.error(e);
				throw e;
			})
			.then((res) => {
				const { data } = res;
				if (data) {
					this.addTokenToStorage('accessToken', data.accessToken);
				}
				return res;
			});
	};

	register = async (credentials: RegisterInput): Promise<APIResponse<RegisterOutput>> => {
		return await this.client
			.POST('/api/auth/register', { body: credentials })
			.catch((e) => {
				console.error(e);
				throw e;
			})
			.then((res) => {
				const { data } = res;
				if (data) {
					this.addTokenToStorage('accessToken', data.accessToken);
					this.addTokenToStorage('refreshToken', data.refreshToken);
				}
				return res;
			});
	};

	private addTokenToStorage = (kind: TokenKind, token: string) => {
		localStorage.setItem(kind, token);
	};
}

export const userClient = new UserClient();
