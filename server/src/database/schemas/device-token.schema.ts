import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { HydratedDocument } from 'mongoose';
import { stellaSchemaOptions } from '../schema-options';

export type DeviceTokenDocument = HydratedDocument<DeviceToken>;

@Schema({ ...stellaSchemaOptions, collection: 'DeviceToken' })
export class DeviceToken {
  @Prop({ type: String, required: true })
  _id!: string;

  @Prop({ required: true, unique: true })
  deviceId!: string;

  @Prop({ required: true })
  fcmToken!: string;

  @Prop({ type: Date, required: true })
  updatedAt!: Date;
}

export const DeviceTokenSchema = SchemaFactory.createForClass(DeviceToken);
