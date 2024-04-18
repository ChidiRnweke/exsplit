import type { components as UserComponents } from '../schemas/exsplit.spec.UserService';

export type LoginInput = UserComponents['schemas']['LoginRequestContent'];
export type LoginOutput = UserComponents['schemas']['LoginResponseContent'];
export type RegisterInput = UserComponents['schemas']['RegisterRequestContent'];
export type RegisterOutput = UserComponents['schemas']['RegisterResponseContent'];
export type RefreshInput = UserComponents['schemas']['RefreshRequestContent'];
export type RefreshOutput = UserComponents['schemas']['RefreshResponseContent'];
export type UserId = { userId: string };
