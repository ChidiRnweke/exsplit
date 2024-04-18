/**
 * This file was auto-generated by openapi-typescript.
 * Do not make direct changes to the file.
 */

export interface paths {
	'/api/auth/login': {
		post: operations['Login'];
	};
	'/api/auth/refresh': {
		post: operations['Refresh'];
	};
	'/api/auth/register': {
		post: operations['Register'];
	};
}

export type webhooks = Record<string, never>;

export interface components {
	schemas: {
		AuthErrorResponseContent: {
			message: string;
		};
		LoginRequestContent: {
			email: string;
			password: string;
		};
		LoginResponseContent: {
			accessToken: string;
			refreshToken: string;
		};
		RefreshRequestContent: {
			refreshToken: string;
		};
		RefreshResponseContent: {
			accessToken: string;
		};
		RegisterRequestContent: {
			email: string;
			password: string;
		};
		RegisterResponseContent: {
			userId: string;
			refreshToken: string;
			accessToken: string;
		};
		ValidationErrorResponseContent: {
			message: string;
		};
	};
	responses: never;
	parameters: never;
	requestBodies: never;
	headers: never;
	pathItems: never;
}

export type $defs = Record<string, never>;

export type external = Record<string, never>;

export interface operations {
	Login: {
		requestBody: {
			content: {
				'application/json': components['schemas']['LoginRequestContent'];
			};
		};
		responses: {
			/** @description Login 200 response */
			200: {
				content: {
					'application/json': components['schemas']['LoginResponseContent'];
				};
			};
			/** @description ValidationError 400 response */
			400: {
				content: {
					'application/json': components['schemas']['ValidationErrorResponseContent'];
				};
			};
		};
	};
	Refresh: {
		requestBody: {
			content: {
				'application/json': components['schemas']['RefreshRequestContent'];
			};
		};
		responses: {
			/** @description Refresh 200 response */
			200: {
				content: {
					'application/json': components['schemas']['RefreshResponseContent'];
				};
			};
			/** @description ValidationError 400 response */
			400: {
				content: {
					'application/json': components['schemas']['ValidationErrorResponseContent'];
				};
			};
			/** @description AuthError 401 response */
			401: {
				content: {
					'application/json': components['schemas']['AuthErrorResponseContent'];
				};
			};
		};
	};
	Register: {
		requestBody: {
			content: {
				'application/json': components['schemas']['RegisterRequestContent'];
			};
		};
		responses: {
			/** @description Register 200 response */
			200: {
				content: {
					'application/json': components['schemas']['RegisterResponseContent'];
				};
			};
			/** @description ValidationError 400 response */
			400: {
				content: {
					'application/json': components['schemas']['ValidationErrorResponseContent'];
				};
			};
		};
	};
}
