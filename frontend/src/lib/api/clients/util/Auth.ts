import type { Middleware } from 'openapi-fetch';
import { userClient } from '../UserService';
import { AuthenticationError } from './ErrorHandling';
import { jwtDecode } from 'jwt-decode';

type JWTToken = {
	exp: number;
	subj: string;
};

/**
 * Middleware that adds the Authorization header to requests.
 *
 *  * If the access token is expired, it will try to refresh it using the refresh token.
 *  * If the refresh token is expired, it will throw an AuthenticationError.
 *  * This error can be caught and used to redirect the user to the login page.
 */
class AuthMiddleware implements Middleware {
	private userClient = userClient;

	onRequest = async (req: Request) => {
		const accessToken = await this.findAndValidateToken();
		req.headers.set('Authorization', `Bearer ${accessToken}`);
		return req;
	};

	private decodeToken = (token: string): JWTToken | null => {
		try {
			return jwtDecode<JWTToken>(token);
		} catch (e) {
			console.error('Token decoding failed:', e);
			return null;
		}
	};

	private tokenIsValid = (token: JWTToken | null, leeway = 10): boolean => {
		if (!token) {
			return false;
		}
		const nowSeconds = Date.now() / 1000;
		return token.exp > nowSeconds + leeway;
	};

	private findAndValidateToken = async (): Promise<string> => {
		const accessToken = localStorage.getItem('accessToken');
		const refreshToken = localStorage.getItem('refreshToken');

		if (accessToken && this.tokenIsValid(this.decodeToken(accessToken))) {
			return accessToken;
		} else if (refreshToken && this.tokenIsValid(this.decodeToken(refreshToken))) {
			const newAccessToken = await this.getNewAccessToken(refreshToken);
			localStorage.setItem('accessToken', newAccessToken);
			return newAccessToken;
		} else {
			throw new AuthenticationError('Please log in again.');
		}
	};

	private getNewAccessToken = async (refreshToken: string): Promise<string> => {
		const { data, error } = await this.userClient.refresh({ refreshToken });
		if (data) {
			return data.accessToken;
		} else {
			// The error messages can be used as context to determine the error type
			console.log(error.message);
			throw new AuthenticationError('Please log in again.');
		}
	};
}

/**
 * Middleware that adds the Authorization header to requests.
 *
 *  * If the access token is expired, it will try to refresh it using the refresh token.
 *  * If the refresh token is expired, it will throw an AuthenticationError.
 *  * This error can be caught and used to redirect the user to the login page.
 */
export const authMiddleware = new AuthMiddleware();
