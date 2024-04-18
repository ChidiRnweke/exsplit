<script>
	import H1 from '../components/shared/H1.svelte';
	import { UserClient } from '$lib/services/UserService';
	const login = async () => {
		const userClient = new UserClient();
		await userClient.login({ email: 'test@foo.com', password: 'password' });
	};
</script>

<section class="text-center">
	<H1>Manage your expenses with EXsplit.</H1>
	<button on:click={login}>Login</button>
</section>
